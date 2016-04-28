Hierarchical Test Cases
=======================

*WARNING* Brendan wrote this and would be the first to acknowledge that it's a pretty opinionated piece of writing. If 
you think it's crazy or ill-informed then please come and tell him so!

Most Java test classes look like this

    public class MyTest
    {
        @Test
        public void testMyClassFunction
        {
            assertSomething(...);
        }
    }

That all works fine until the class under test becomes slightly more interesting (ie: more than about a single 
method!). Then you will want to start to do things like:
* namespace your tests so that it's obvious exactly what your testing, eg: testFooReturnsXxxWhenYYY; and/or
* partitioning your setup logic to keep test cases interfering with each other and/or doing unecessary work.

The first gets pretty tedious, pretty fast. The second isn't really solvable while staying DRY and/or following the 
usually accepted unit-testing wisdom of limiting the number of assertions in a test case.

The alternative is to use some scheme to (hierarchically) partition your test cases.  Below are some options for doing
that.  I'm not sure which one of these approaches is better.  The code base has examples of both techniques though 
work-around mentioned below to the Enclosed 'inherited' state limitations has not been used in anger.  I suspect that 
if further limitations appear with the HierarchicalContextRunner then we will have to use the Enclosed technique.

HierarchicalContextRunner
-------------------------

This is an externally developed TestRunner that allows you to hierarchically structure your test cases using (**non**-
static) inner classes.  eg:

    @RunWith(HierarchicalContextRunner.class)
    public class MyTest
    {
    
        private Object someRootLevelObjectCommonToAllChildTestCases;
        
        @Before
        public void setUp()
        {
            // root-level setup
            someRootLevelObjectCommonToAllChildTestCases = new Object();
            ...
        }
        
        public class Wibble
        {
        
            @Before
            public void setUp()
            {
                // setup specific to the 'Wibble' context
                ...
            }
            
            @Test
            public void whenFooReturnsBar()
            {
                ...
            }
            
            @Test
            public void throwsExceptionWhenReceivesNull()
            {
                ...
            } 
        }
    }

The runner will ensure that outer @Before and @After blocks are run in the right order (outside-in and inside-out
respectively).

**Pros**:

* 'container'-based hierarchy keeps setUp/tearDowns and fields suitably DRY and compartmentalised.

**Cons**:

* External dependency
* Must use a custom runner, preventing use of other useful runners like PowerMockRunner and the Spring runner (see
  notes below about these two runners).
* Eclipse cannot run a nested test case by itself (this is pretty crap when debugging but not such an issue otherwise).
* Eclipse reports odd numbers of test cases (I'm not blaming Eclipse for either of these bugs actually).
* There is a bug preventing this class (or the neseted classes) inheriting from other test-case classes.

Enclosed
--------

There is a specific JUnit runner known as the Enclosed runner that can be used as follows:

    @RunWith(Enclosed.class)
    public class MyTest
    {

        public static class Wibble
        {
        
            @Test
            public void whenFooReturnsBar()
            {
                ...
            }
            
            @Test
            public void throwsExceptionWhenReceivesNull()
            {
                ...
            } 


        }
    }

The runner relies on the natural partitioning that static inner classes provide.

**Pros**:

* Standard (if 'experimental' JUnit runner)
* Nested classes can use other runners if required (eg: Parameterized)

**Cons**:
* Must use a custom runner, preventing use of other useful runners like PowerMockRunner and the Spring runner (see
  notes below about these two runners).
* Static classes do not 'inherit' state from the containing class, meaning common variables cannot be shared.  ie: this
  technique really only solves the namespace problem.

The other runners limitation only applies to 'containing' classes and therefore might be able to be worked-around with 
appropriate use of 'leaf' classes that specify use of a specific runner.

The inherited state issue can be worked-around using the following technique:

    // Must be ignored or the Enclosed runner will complain
    @Ignore
    public abstract static class ClassWithState
    {
        protected String message;
        public void setUp() { message = "ClassWithState"; }
    }

    public static class WeeTest extends ClassWithState
    {
        protected String anotherMessage;
        
        @Before
        public void setUp()
        {
            // Must manually call super to run the setup (party like it's 1999!)
            super.setUp();
            anotherMessage = "WeeTest";
        }
        @Test public void test() 
        { 
            assertEquals("ClassWithState", message); 
            assertEquals("WeeTest", anotherMessage);
        }
    }


Use JUnit Suites
----------------

In theory, something similar to the nested classes could be achieved through the use of the Suite runner.  I haven't
tried this out in practice but it would involve static nested classes that inherit from each other very similar to the
nested classes work-around mentioned above.


PowerMockRunner
===============

When test classes need to mock static classes and/or methods then they *must* be run using @RunWith(PowerMockRunner).
I believe the reason for this is that PowerMock needs to intercept and manipulate loaded classes and run tests with
a non-standard ClassLoader (so that the mocked classes are retrieved rather than the un-mocked ones) - this is, I think,
the reason why there needs to be a @PrepareForTest annotation specifying classes that will be mocked.

PowerMock supplies a special @Rule that is supposed to replace the need to use @RunWith(PowerMockRunner) but I could
not get that to work.  Consequently, if PowerMock is needed and you also want to hierarchically arrange your tests then
you will probably need to use PowerMockRunner on specific nested test classes with Enclosed runners being used outside
of that test case.  I haven't actually tried this in practice to see if it works.

There are a couple of alternatives to static mocking:
* Don't call static methods - inject 'service' objects that provide non-static methods to use instead.
* Wrap static method calls in an instance method that *can* be stubbed.
* Roll your own replacement of static variables (or specify it as an @Rule).
(The continuum CommandLineImporterTest uses both of the last two techniques.)

Postscript: I've now managed to avoid using PowerMock at all.  The better way is to design around needing to use static
methods, or static objects (and in the odd case where you have to use static objects then reflection can be used to 
replace those with mocks).  Of course, some static methods can't really be mocked, eg: File, Path, etc. methods but in
those cases writing unit tests that actually deal with real objects is not turning out to be too hard.

SpringJUnit4ClassRunner
=======================

Luckily, there is a fairly simple work-around to using SpringJUnit4ClassRunner:

    @RunWith(SomeNonSpringRunner.class)
    @ContextConfiguration(classes = { MyConfigClass.class })
    public class Test
    {
        @Before
        public void setUp() throws Exception
        {
            TestContextManager testContextManager = new TestContextManager(getClass());
            testContextManager.prepareTestInstance(this);
            ...
        }
    }

However, you may find that a field in a test case can't be @Autowired with this technique, eg:

        // Barfs!
        @Autowired
        SomeComponent myComponent;
        
The work around is to autowire it yourself:

    @RunWith(SomeNonSpringRunner.class)
    @ContextConfiguration(classes = { MyConfigClass.class })
    public class Test
    {
        @Autowired
        private ConfigurableApplicationContext context;

        private SomeComponent myComponent;
        
        @Before
        public void setUp() throws Exception
        {
            TestContextManager testContextManager = new TestContextManager(getClass());
            testContextManager.prepareTestInstance(this);
            ...
            myComponent = new MyComponent();
            context.getBeanFactory().autowireBean(myComponent);
            // or
            myComponent = context.getBeanFactory().createBean(MyComponent.class);
        }
    }

Please see DynamicInstantiationAwareBeanPostProcessor for information on how to control objects injected into the
component.  (The technique previously described here, using registerResolvableDependency on a 
ConfigurableApplicationContext's BeanFactory only worked for Singleton objects.)
