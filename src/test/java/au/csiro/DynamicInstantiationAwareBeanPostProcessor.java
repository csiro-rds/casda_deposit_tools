package au.csiro;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/*
 * #%L
 * CSIRO ASKAP Science Data Archive
 * %%
 * Copyright (C) 2015 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
 * %%
 * Licensed under the CSIRO Open Source License Agreement (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file.
 * #L%
 */

/**
 * This is an implementation of InstantiationAwareBeanPostProcessor that can be used to override specific
 * beans when Spring is asked to wire-up an object.  The rationale for doing this is that in some test cases we 
 * want to override how Spring wires up objects, eg: using mock objects.  Some solutions and ideas are discussed here:
 * http://stackoverflow.com/questions/565334/spring-beans-redefinition-in-unit-test-environment
 * Some people use different Spring application context definitions for different test classes, some people use
 * Mockito's InjectMocks, and some simply revert to private field setting.  The later has one significant benefit over
 * the other two in that it can be applied selectively on a test-case by test-case basis (the others will typically
 * apply to all test cases in a test class).  This class provides another alternative that tries to do things in a
 * more Spring-like way than setting a private field - by supporting the custom supply of specific beans when an
 * object is being wired-up.  Unfortunately, Spring seems to make it quite difficult (or at least non-obvious) to do 
 * this.
 * <p>
 * We may (rapidly?) decide that this overkill versus the simple:
 * <pre>
 * <code>
 *      ReflectionTestUtils.setField(myBean, "someComponent", someComponentOverride);
 * </code>
 * </pre>
 * <p>
 * This object can be used in test cases as follows:
 * <pre>
 * <code>
 *      @Autowired
 *      private ConfigurableApplicationContext context; // required to wire-up custom-wired objects
 * 
 *      @Autowired
 *      private DynamicInstantiationAwareBeanPostProcessor beanPostProcessor; // an instance of this class
 * 
 *      @Before
 *      public void setUp()
 *      {
 *          // Manual equivalent to @RunWith(SpringJUnit4ClassRunner.class)
 *          TestContextManager testContextManager = new TestContextManager(getClass());
 *          testContextManager.prepareTestInstance(this);
 *          // Add the post processor to the bean factory
 *          context.getBeanFactory().addBeanPostProcessor(beanPostProcessor);
 *          ...
 *      }
 *      
 *      @After
 *      public void tearDown()
 *      {
 *          ...
 *          // Reset the bean processor (otherwise your custom wiring will affect other test cases!!!)
 *          beanPostProcessor.reset();
 *          ...
 *      }
 *      
 *          // Somewhere in code
 *          
 *          // Create your own object and specify it as an override:
 *          SomeObject = ...;
 *          beanPostProcessor.overrideBean(SomeObject.class, someObject);
 *          
 *          // Wire-up your dependent object
 *          MySpringBean bean = context.getBeanFactory().createBean(MySpringBean.class);
 *          // or 
 *          MyNonSpringObject object = new MyNonSpringObject();
 *          context.getBeanFactory().autowireBean(object);
 * </code>
 * </pre>
 * <p>
 * At this stage the class only provides support for specifying beans by class but that could be extended to
 * support beans by name as well.
 * <p>
 * <b>Note:</b> This class has itself been declared as a Spring Singleton and it should be instantiated in a test 
 * application config.  (The reason for this is that implementation of ConfigurableBeanFactory does not allow 
 * BeanPostProcessors to be removed from the factory - though it is smart enough to detect when the same one is being 
 * added and will remove it first.)
 * <p>
 * Copyright 2014, CSIRO Australia
 * All rights reserved.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class DynamicInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
{

    private Map<Class<? extends Object>, Object> overrides = new HashMap<>();
    
    /**
     * Registers a specific object to override the given beanClass.
     * @param beanClass
     * @param bean
     */
    public void overrideBean(Class<? extends Object> beanClass, Object bean)
    {
        overrides.put(beanClass, bean);
    }

    /**
     * Resets all bean overrides.
     */
    public void reset()
    {
        overrides.clear();
    }

    /**
     * Extension of postProcessAfterInitialization that returns an overriding bean if one has been registered.
     * <p>
     * It would make more sense if this was postProcessBeforeInstantiation but that method only supports coarse-
     * grained replacement of beans.
     */
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        /*
         * We have to walk up the object's inheritance hierarchy looking for matches since in many cases the
         * object will be a proxy.
         */
        Class<? extends Object> realClass = bean.getClass();
        while(realClass != null)
        {
            if (overrides.containsKey(realClass))
            {
                return overrides.get(realClass);
            }
            else
            {
                realClass = realClass.getSuperclass();
            }
        }
        return super.postProcessAfterInitialization(bean, beanName);
    }
}
