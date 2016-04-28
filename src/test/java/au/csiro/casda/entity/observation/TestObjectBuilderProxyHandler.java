package au.csiro.casda.entity.observation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

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
 * Base (abstract) class that partially implements an InvocationHandler for TestObjectBuilders. Subclasses will
 * typically extend this class to provide default values for constructed objects and to perform any additional
 * post-creation wiring-up.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public abstract class TestObjectBuilderProxyHandler<T> implements InvocationHandler
{
    private Map<Method, Object[]> setters = new LinkedHashMap<Method, Object[]>();

    /**
     * Method typically used in subclass constructors to configure default values for an object under construction.
     * 
     * @param fieldName
     *            the name of the field on the object as exposed by a setter, eg: for a setter 'setFoo' the fieldName
     *            would be 'foo'
     * 
     * @param values
     *            the value or values to set on the constructed object using the setter matching the given fieldName
     *            (the value is usually the real value but if a TestObjectBuilder is supplied then a value will be set
     *            by calling that builder's build method)
     */
    protected void setDefault(String fieldName, Object... values)
    {
        try
        {
            String methodName = "set" + StringUtils.capitalize(fieldName);
            Method method = null;
            Method[] methods = getBuilderClass().getMethods();
            for (Method aMethod : methods)
            {
                if (methodName.equals(aMethod.getName()))
                {
                    method = aMethod;
                    break;
                }
            }
            if (method == null)
            {
                throw new RuntimeException("No such method " + methodName + " for class " + getBuilderClass().getName());
            }
            setters.put(method, values);
        }
        catch (SecurityException e)
        {
            throw new RuntimeException(e);
        }

    }

    /**
     * Method used in subclasses when they need to find out whether a particular field as set via a default or explicit
     * setter call.
     * 
     * @param fieldName
     *            the name of the field on the object as exposed by a setter, eg: for a setter 'setFoo' the fieldName
     *            would be 'foo'
     * @return whether the field was or will be set
     */
    protected boolean didBuild(String fieldName)
    {
        String methodName = "set" + StringUtils.capitalize(fieldName);
        return this.setters.keySet().stream().anyMatch((m) -> m.getName().equals(methodName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if ("build".equals(method.getName()))
        {
            T object = createObject();
            populateFields(object);
            return object;
        }
        else if (Pattern.matches("^set[A-Z].*$", method.getName()))
        {
            setters.put(method, args);
            return proxy;
        }
        else
        {
            throw new RuntimeException("Unhandled method: " + method);
        }
    }

    /**
     * Template method that subclasses must implement by returning the TestObjectBuilder class that this handler will
     * provide behaviour for.
     * 
     * @return a TestObjectBuilder<T>
     */
    protected abstract Class<? extends TestObjectBuilder<T>> getBuilderClass();

    /**
     * Template method that subclasses must implement by returning a (typically clean) instance of the object under
     * construction.
     * 
     * @return a T
     */
    protected abstract T createObject();

    /**
     * Populates all the fields in a constructed object using values provided by setDefault or an explicit
     * TestObjectBuilder setter method. Sublasses may extend this method to configure additional relationships or values
     * that cannot sensibly be supplied via a default or explicit setter.
     * 
     * @param object
     *            a T
     */
    protected void populateFields(T object)
    {
        for (Method setter : this.setters.keySet())
        {
            invokeMatchingSetter(object, setter, this.setters.get(setter));
        }
    }

    private void invokeMatchingSetter(Object object, Method setter, Object[] values)
    {
        try
        {
            for (int i = 0; i < values.length; i++)
            {
                if (values[i] instanceof TestObjectBuilder<?>)
                {
                    values[i] = ((TestObjectBuilder<?>) values[i]).build();
                }
            }
            object.getClass().getMethod(setter.getName(), setter.getParameterTypes()).invoke(object, values);
        }
        catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }
}
