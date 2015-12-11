//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.http.pathmap;

import static org.hamcrest.Matchers.notNullValue;

import org.junit.Assert;
import org.junit.Test;

public class PathMappingsTest
{
    private void assertMatch(PathMappings<String> pathmap, String path, String expectedValue)
    {
        String msg = String.format(".getMatch(\"%s\")",path);
        MappedResource<String> match = pathmap.getMatch(path);
        Assert.assertThat(msg,match,notNullValue());
        String actualMatch = match.getResource();
        Assert.assertEquals(msg,expectedValue,actualMatch);
    }

    public void dumpMappings(PathMappings<String> p)
    {
        for (MappedResource<String> res : p)
        {
            System.out.printf("  %s%n",res);
        }
    }

    /**
     * Test the match order rules with a mixed Servlet and regex path specs
     * <p>
     * <ul>
     * <li>Exact match</li>
     * <li>Longest prefix match</li>
     * <li>Longest suffix match</li>
     * </ul>
     */
    @Test
    public void testMixedMatchOrder()
    {
        PathMappings<String> p = new PathMappings<>();

        p.put(new ServletPathSpec("/"),"default");
        p.put(new ServletPathSpec("/animal/bird/*"),"birds");
        p.put(new ServletPathSpec("/animal/fish/*"),"fishes");
        p.put(new ServletPathSpec("/animal/*"),"animals");
        p.put(new RegexPathSpec("^/animal/.*/chat$"),"animalChat");
        p.put(new RegexPathSpec("^/animal/.*/cam$"),"animalCam");
        p.put(new RegexPathSpec("^/entrance/cam$"),"entranceCam");

        // dumpMappings(p);

        assertMatch(p,"/animal/bird/eagle","birds");
        assertMatch(p,"/animal/fish/bass/sea","fishes");
        assertMatch(p,"/animal/peccary/javalina/evolution","animals");
        assertMatch(p,"/","default");
        assertMatch(p,"/animal/bird/eagle/chat","animalChat");
        assertMatch(p,"/animal/bird/penguin/chat","animalChat");
        assertMatch(p,"/animal/fish/trout/cam","animalCam");
        assertMatch(p,"/entrance/cam","entranceCam");
    }
    
    /**
     * Test the match order rules imposed by the Servlet API (default vs any)
     */
    @Test
    public void testServletMatchDefault()
    {
        PathMappings<String> p = new PathMappings<>();

        p.put(new ServletPathSpec("/"),"default");
        p.put(new ServletPathSpec("/*"),"any"); 

        assertMatch(p,"/abs/path","any");
        assertMatch(p,"/abs/path/xxx","any");
        assertMatch(p,"/animal/bird/eagle/bald","any");
        assertMatch(p,"/","any");
    }
    
    /**
     * Test the match order rules with a mixed Servlet and URI Template path specs
     * <p>
     * <ul>
     * <li>Exact match</li>
     * <li>Longest prefix match</li>
     * <li>Longest suffix match</li>
     * </ul>
     */
    @Test
    public void testMixedMatchUriOrder()
    {
        PathMappings<String> p = new PathMappings<>();

        p.put(new ServletPathSpec("/"),"default");
        p.put(new ServletPathSpec("/animal/bird/*"),"birds");
        p.put(new ServletPathSpec("/animal/fish/*"),"fishes");
        p.put(new ServletPathSpec("/animal/*"),"animals");
        p.put(new UriTemplatePathSpec("/animal/{type}/{name}/chat"),"animalChat");
        p.put(new UriTemplatePathSpec("/animal/{type}/{name}/cam"),"animalCam");
        p.put(new UriTemplatePathSpec("/entrance/cam"),"entranceCam");

        // dumpMappings(p);

        assertMatch(p,"/animal/bird/eagle","birds");
        assertMatch(p,"/animal/fish/bass/sea","fishes");
        assertMatch(p,"/animal/peccary/javalina/evolution","animals");
        assertMatch(p,"/","default");
        assertMatch(p,"/animal/bird/eagle/chat","animalChat");
        assertMatch(p,"/animal/bird/penguin/chat","animalChat");
        assertMatch(p,"/animal/fish/trout/cam","animalCam");
        assertMatch(p,"/entrance/cam","entranceCam");
    }

    /**
     * Test the match order rules for URI Template based specs
     * <p>
     * <ul>
     * <li>Exact match</li>
     * <li>Longest prefix match</li>
     * <li>Longest suffix match</li>
     * </ul>
     */
    @Test
    public void testUriTemplateMatchOrder()
    {
        PathMappings<String> p = new PathMappings<>();

        p.put(new UriTemplatePathSpec("/a/{var}/c"),"endpointA");
        p.put(new UriTemplatePathSpec("/a/b/c"),"endpointB");
        p.put(new UriTemplatePathSpec("/a/{var1}/{var2}"),"endpointC");
        p.put(new UriTemplatePathSpec("/{var1}/d"),"endpointD");
        p.put(new UriTemplatePathSpec("/b/{var2}"),"endpointE");

        // dumpMappings(p);

        assertMatch(p,"/a/b/c","endpointB");
        assertMatch(p,"/a/d/c","endpointA");
        assertMatch(p,"/a/x/y","endpointC");

        assertMatch(p,"/b/d","endpointE");
    }

}
