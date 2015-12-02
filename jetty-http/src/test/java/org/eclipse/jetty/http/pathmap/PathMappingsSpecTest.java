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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 *
 */
public class PathMappingsSpecTest
{
    @Test
    public void testPathMap() throws Exception
    {
        PathMappings<String> p = new PathMappings<>();

        p.put(new ServletPathSpec("/abs/path"), "1");
        p.put(new ServletPathSpec("/abs/path/longer"), "2");
        p.put(new ServletPathSpec("/animal/bird/*"), "3");
        p.put(new ServletPathSpec("/animal/fish/*"), "4");
        p.put(new ServletPathSpec("/animal/*"), "5");
        p.put(new ServletPathSpec("*.tar.gz"), "6");
        p.put(new ServletPathSpec("*.gz"), "7");
        p.put(new ServletPathSpec("/"), "8");
        // p.put(new ServletPathSpec("/XXX:/YYY"), "9"); // special syntax from Jetty 3.1.x
        p.put(new ServletPathSpec(""), "10");
        p.put(new ServletPathSpec("/\u20ACuro/*"), "11");

        String[][] tests = {
                { "/abs/path", "1"},
                { "/abs/path/xxx", "8"},
                { "/abs/pith", "8"},
                { "/abs/path/longer", "2"},
                { "/abs/path/", "8"},
                { "/abs/path/xxx", "8"},
                { "/animal/bird/eagle/bald", "3"},
                { "/animal/fish/shark/grey", "4"},
                { "/animal/insect/bug", "5"},
                { "/animal", "5"},
                { "/animal/", "5"},
                { "/animal/x", "5"},
                { "/animal/*", "5"},
                { "/suffix/path.tar.gz", "6"},
                { "/suffix/path.gz", "7"},
                { "/animal/path.gz", "5"},
                { "/Other/path", "8"},
                { "/\u20ACuro/path", "11"},
                { "/", "10"},
                };

        for (String[] test : tests)
        {
            assertEquals(test[0], test[1], p.getMatch(test[0]).getResource());
        }

        // assertEquals("Get absolute path", "1", p.get("/abs/path"));
        assertEquals("Match absolute path", "/abs/path", p.getMatch("/abs/path").getPathSpec().pathSpec);
        assertEquals("all matches", "[/animal/bird/*=3, /animal/*=5, *.tar.gz=6, *.gz=7, /=8]",
                p.getMatches("/animal/bird/path.tar.gz").toString());
        assertEquals("Dir matches", "[/animal/fish/*=4, /animal/*=5, /=8]", p.getMatches("/animal/fish/").toString());
        assertEquals("Dir matches", "[/animal/fish/*=4, /animal/*=5, /=8]", p.getMatches("/animal/fish").toString());
        assertEquals("Root matches", "[=10, /=8]",p.getMatches("/").toString());
        assertEquals("Dir matches", "[/=8]", p.getMatches("").toString());

        assertEquals("pathMatch exact", "/Foo/bar", new ServletPathSpec("/Foo/bar").getPathMatch("/Foo/bar"));
        assertEquals("pathMatch prefix", "/Foo", new ServletPathSpec("/Foo/*").getPathMatch("/Foo/bar"));
        assertEquals("pathMatch prefix", "/Foo", new ServletPathSpec("/Foo/*").getPathMatch("/Foo/"));
        assertEquals("pathMatch prefix", "/Foo", new ServletPathSpec("/Foo/*").getPathMatch("/Foo"));
        assertEquals("pathMatch suffix", "/Foo/bar.ext", new ServletPathSpec("*.ext").getPathMatch("/Foo/bar.ext"));
        assertEquals("pathMatch default", "/Foo/bar.ext", new ServletPathSpec("/").getPathMatch("/Foo/bar.ext"));

        assertEquals("pathInfo exact", null, new ServletPathSpec("/Foo/bar").getPathInfo("/Foo/bar"));
        assertEquals("pathInfo prefix", "/bar", new ServletPathSpec("/Foo/*").getPathInfo("/Foo/bar"));
        assertEquals("pathInfo prefix", "/*", new ServletPathSpec("/Foo/*").getPathInfo("/Foo/*"));
        assertEquals("pathInfo prefix", "/", new ServletPathSpec("/Foo/*").getPathInfo("/Foo/"));
        assertEquals("pathInfo prefix", null, new ServletPathSpec("/Foo/*").getPathInfo("/Foo"));
        assertEquals("pathInfo suffix", null, new ServletPathSpec("*.ext").getPathInfo("/Foo/bar.ext"));
        assertEquals("pathInfo default", null, new ServletPathSpec("/").getPathInfo("/Foo/bar.ext"));
        
        // assertEquals("multi paths", "9", p.getMatch("/XXX").getResource());
        // assertEquals("multi paths", "9", p.getMatch("/YYY").getResource());

        p.put(new ServletPathSpec("/*"), "0");

        // assertEquals("Get absolute path", "1", p.get("/abs/path"));
        assertEquals("Match absolute path", "/abs/path", p.getMatch("/abs/path").getPathSpec().pathSpec);
        assertEquals("Match absolute path", "1", p.getMatch("/abs/path").getResource());
        assertEquals("Mismatch absolute path", "0", p.getMatch("/abs/path/xxx").getResource());
        assertEquals("Mismatch absolute path", "0", p.getMatch("/abs/pith").getResource());
        assertEquals("Match longer absolute path", "2", p.getMatch("/abs/path/longer").getResource());
        assertEquals("Not exact absolute path", "0", p.getMatch("/abs/path/").getResource());
        assertEquals("Not exact absolute path", "0", p.getMatch("/abs/path/xxx").getResource());

        assertEquals("Match longest prefix", "3", p.getMatch("/animal/bird/eagle/bald").getResource());
        assertEquals("Match longest prefix", "4", p.getMatch("/animal/fish/shark/grey").getResource());
        assertEquals("Match longest prefix", "5", p.getMatch("/animal/insect/bug").getResource());
        assertEquals("mismatch exact prefix", "5", p.getMatch("/animal").getResource());
        assertEquals("mismatch exact prefix", "5", p.getMatch("/animal/").getResource());

        assertEquals("Match longest suffix", "0", p.getMatch("/suffix/path.tar.gz").getResource());
        assertEquals("Match longest suffix", "0", p.getMatch("/suffix/path.gz").getResource());
        assertEquals("prefix rather than suffix", "5", p.getMatch("/animal/path.gz").getResource());

        assertEquals("default", "0", p.getMatch("/Other/path").getResource());

        assertEquals("pathMatch /*", "", new ServletPathSpec("/*").getPathMatch("/xxx/zzz"));
        assertEquals("pathInfo /*", "/xxx/zzz", new ServletPathSpec("/*").getPathInfo("/xxx/zzz"));

        assertTrue("match /", new ServletPathSpec("/").matches("/anything"));
        assertTrue("match /*", new ServletPathSpec("/*").matches("/anything"));
        assertTrue("match /foo", new ServletPathSpec("/foo").matches("/foo"));
        assertTrue("!match /foo", !new ServletPathSpec("/foo").matches("/bar"));
        assertTrue("match /foo/*", new ServletPathSpec("/foo/*").matches("/foo"));
        assertTrue("match /foo/*", new ServletPathSpec("/foo/*").matches("/foo/"));
        assertTrue("match /foo/*", new ServletPathSpec("/foo/*").matches("/foo/anything"));
        assertTrue("!match /foo/*", !new ServletPathSpec("/foo/*").matches("/bar"));
        assertTrue("!match /foo/*", !new ServletPathSpec("/foo/*").matches("/bar/"));
        assertTrue("!match /foo/*", !new ServletPathSpec("/foo/*").matches("/bar/anything"));
        assertTrue("match *.foo", new ServletPathSpec("*.foo").matches("anything.foo"));
        assertTrue("!match *.foo", !new ServletPathSpec("*.foo").matches("anything.bar"));

        assertEquals("match / with ''", "10", p.getMatch("/").getResource());
        
        assertTrue("match \"\"", new ServletPathSpec("").matches("/"));
    }

    /**
     * See JIRA issue: JETTY-88.
     * @throws Exception failed test
     */
    @Test
    public void testPathMappingsOnlyMatchOnDirectoryNames() throws Exception
    {
        String spec = "/xyz/*";

        assertMatch(spec, "/xyz");
        assertMatch(spec, "/xyz/");
        assertMatch(spec, "/xyz/123");
        assertMatch(spec, "/xyz/123/");
        assertMatch(spec, "/xyz/123.txt");
        assertNotMatch(spec, "/xyz123");
        assertNotMatch(spec, "/xyz123;jessionid=99");
        assertNotMatch(spec, "/xyz123/");
        assertNotMatch(spec, "/xyz123/456");
        assertNotMatch(spec, "/xyz.123");
        assertNotMatch(spec, "/xyz;123"); // as if the ; was encoded and part of the path
        assertNotMatch(spec, "/xyz?123"); // as if the ? was encoded and part of the path
    }

    @Test
    public void testPrecidenceVsOrdering() throws Exception
    {
        PathMappings<String> p = new PathMappings<>();
        p.put(new ServletPathSpec("/dump/gzip/*"),"prefix");
        p.put(new ServletPathSpec("*.txt"),"suffix");
       
        assertEquals(null,p.getMatch("/foo/bar"));
        assertEquals("prefix",p.getMatch("/dump/gzip/something").getResource());
        assertEquals("suffix",p.getMatch("/foo/something.txt").getResource());
        assertEquals("prefix",p.getMatch("/dump/gzip/something.txt").getResource());
        
        p = new PathMappings<>();
        p.put(new ServletPathSpec("*.txt"),"suffix");
        p.put(new ServletPathSpec("/dump/gzip/*"),"prefix");
       
        assertEquals(null,p.getMatch("/foo/bar"));
        assertEquals("prefix",p.getMatch("/dump/gzip/something").getResource());
        assertEquals("suffix",p.getMatch("/foo/something.txt").getResource());
        assertEquals("prefix",p.getMatch("/dump/gzip/something.txt").getResource());
    }
    
    
    
    private void assertMatch(String spec, String path)
    {
        boolean match = new ServletPathSpec(spec).matches(path);
        assertTrue("PathSpec '" + spec + "' should match path '" + path + "'", match);
    }

    private void assertNotMatch(String spec, String path)
    {
        boolean match = new ServletPathSpec(spec).matches(path);
        assertFalse("PathSpec '" + spec + "' should not match path '" + path + "'", match);
    }
}
