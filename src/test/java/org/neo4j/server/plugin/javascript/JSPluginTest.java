/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.plugin.javascript;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.plugin.javascript.JSPlugin;
import org.neo4j.server.rest.repr.OutputFormat;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.formats.JsonFormat;
import org.neo4j.test.ImpermanentGraphDatabase;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;

public class JSPluginTest
{
    private static ImpermanentGraphDatabase neo4j = null;
    private static JSPlugin plugin = null;
    private static OutputFormat json = null;
    private static JSONParser parser = new JSONParser();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        json = new OutputFormat( new JsonFormat(),
                new URI( "http://localhost/" ), null );
        neo4j = new ImpermanentGraphDatabase();
        plugin = new JSPlugin();
        Graph graph = new Neo4jGraph( neo4j );
        
        Vertex marko = graph.addVertex( "1" );
        marko.setProperty( "name", "marko" );
        marko.setProperty( "age", 29 );

        Vertex vadas = graph.addVertex( "2" );
        vadas.setProperty( "name", "vadas" );
        vadas.setProperty( "age", 27 );

        Vertex lop = graph.addVertex( "3" );
        lop.setProperty( "name", "lop" );
        lop.setProperty( "lang", "java" );

        Vertex josh = graph.addVertex( "4" );
        josh.setProperty( "name", "josh" );
        josh.setProperty( "age", 32 );

        Vertex ripple = graph.addVertex( "5" );
        ripple.setProperty( "name", "ripple" );
        ripple.setProperty( "lang", "java" );

        Vertex peter = graph.addVertex( "6" );
        peter.setProperty( "name", "peter" );
        peter.setProperty( "age", 35 );

        graph.addEdge( "7", marko, vadas, "knows" ).setProperty( "weight", 0.5f );
        graph.addEdge( "8", marko, josh, "knows" ).setProperty( "weight", 1.0f );
        graph.addEdge( "9", marko, lop, "created" ).setProperty( "weight", 0.4f );
        graph.addEdge( "10", josh, ripple, "created" ).setProperty( "weight",1.0f );
        graph.addEdge( "11", josh, lop, "created" ).setProperty( "weight", 0.4f );
        graph.addEdge( "12", peter, lop, "created" ).setProperty( "weight",0.2f );
    }

    private String exampleURI = "https://github.com/tinkerpop/gremlin/raw/master/data/graph-example-1.xml";

    @After
    public void tearDown() throws Exception
    {
    }

        
    @Test
    public void testExecuteScriptVertex() throws Exception
    {
    	//String script = "g.v(1)";
    	String script = "pipe.start(g.getVertex(1)).next()";
    	Representation rep = JSPluginTest.executeTestScript( script , null);
    	String JSONString = json.format( rep );
    	
        JSONObject object = (JSONObject) parser.parse( JSONString );
        
        Assert.assertEquals( 29l,
                ( (JSONObject) object.get( "data" ) ).get( "age" ) );
        Assert.assertEquals( "marko",
                ( (JSONObject) object.get( "data" ) ).get( "name" ) );
    }
    
    @Test
    public void testReturnTable() throws Exception
    {
//    	String script = "t = new Table();" +
//        				"g.v(1).out('knows').as('friends').table(t).iterate();" +
//        				"return t";
    	String script = "t = new Table();" +
						"pipe.start(g.getVertex(1)).out('knows').as('friends').table(t).iterate();" +
						"t";
    	Representation rep = JSPluginTest.executeTestScript( script, null);
    	String out = json.format(rep);
    	System.out.println(out);
        assertTrue( out.contains("josh") );
    }

    @Test
    public void testExecuteScriptVertices() throws Exception
    {
    	//String script = "g.V";
    	String script = "pipe.start(g).V()";
        JSONArray array = (JSONArray) parser.parse( json.format( JSPluginTest.executeTestScript( script, null) ) );
        List<String> ids = new ArrayList<String>( Arrays.asList( "1", "2", "3", "4", "5", "6" ) );
        Assert.assertEquals( array.size(), 6 );
        for ( Object object : array )
        {
            String self = (String) ( (JSONObject) object ).get( "self" );
            String id = self.substring( self.lastIndexOf( "/" ) + 1 );
            ids.remove( id );
            String name = (String) ( (JSONObject) ( (JSONObject) object ).get( "data" ) ).get( "name" );
            if ( id.equals( "1" ) )
            {
                Assert.assertEquals( name, "marko" );
            }
            else if ( id.equals( "2" ) )
            {
                Assert.assertEquals( name, "vadas" );
            }
            else if ( id.equals( "3" ) )
            {
                Assert.assertEquals( name, "lop" );
            }
            else if ( id.equals( "4" ) )
            {
                Assert.assertEquals( name, "josh" );
            }
            else if ( id.equals( "5" ) )
            {
                Assert.assertEquals( name, "ripple" );
            }
            else if ( id.equals( "6" ) )
            {
                Assert.assertEquals( name, "peter" );
            }
            else
            {
                Assert.assertTrue( false );
            }

        }
        Assert.assertEquals( ids.size(), 0 );
    }

    @Test
    public void testExecuteScriptEdges() throws Exception
    {
    	//String script = "g.E";
    	String script = "pipe.start(g).E()";
        JSONArray array = (JSONArray) parser.parse(  json.format( JSPluginTest.executeTestScript( script, null) ) );
        List<String> ids = new ArrayList<String>( Arrays.asList( "0", "1", "2",
                "3", "4", "5" ) );
        Assert.assertEquals( array.size(), 6 );
        for ( Object object : array )
        {
            String self = (String) ( (JSONObject) object ).get( "self" );
            String id = self.substring( self.lastIndexOf( "/" ) + 1 );
            ids.remove( id );
            Double weight = (Double) ( (JSONObject) ( (JSONObject) object ).get( "data" ) ).get( "weight" );
            Assert.assertNotNull( weight );
            Assert.assertTrue( weight > 0.1 );
        }
        Assert.assertEquals( ids.size(), 0 );
    }

    @Test
    public void testExecuteScriptGraph() throws Exception
    {
        String ret = (String) parser.parse( json.format( JSPluginTest.executeTestScript( "g", null) ) );
        Assert.assertEquals( ret, "ImpermanentGraphDatabase [" + neo4j.getStoreDir() + "]" );
    }

    @Test
    public void testExecuteScriptLong() throws Exception
    {
//        Assert.assertEquals(
//                1L,
//                parser.parse( json.format( GremlinPluginTest.executeTestScript( "1", null) ) ) );
        
    	// NOTE: The Rhino script engine converts JavaScript numbers to Java doubles
        Assert.assertEquals(
                1.0,
                parser.parse( json.format( JSPluginTest.executeTestScript( "1", null) ) ) );
    }

    @Test
    public void testExecuteScriptLongs()
    {
//        Assert.assertEquals("[ 1, 2, 5, 6, 8 ]",
//    			json.format( GremlinPluginTest.executeTestScript( "[1,2,5,6,8]", null) ) );
        
    	// NOTE: The Rhino script engine converts JavaScript numbers to Java doubles
        Assert.assertEquals("[ 1.0, 2, 5, 6, 8 ]",
                			json.format( JSPluginTest.executeTestScript( "[1,2,5,6,8]", null) ) );
        
    }

    @Test
    public void testExecuteScriptNull()
    {
    	//String script = "for(i in 1..2){g.v(0)}";
    	String script = "for(i =0; i < 2; i++){ pipe.start(g.getVertex(0)).iterate() }";
   
        Assert.assertEquals(
                "\"null\"",
                json.format( JSPluginTest.executeTestScript( script, null) ) );
    }

    @Test
    public void testExecuteScriptParams() throws ParseException
    {
        Assert.assertEquals(
                "1",
                json.format( JSPluginTest.executeTestScript( "x", (Map)parser.parse( "{\"x\" : 1}")) ) );
    }
    
    @Test
    public void testExecuteScriptEmptyParams() throws ParseException
    {
    	// All numeric values are returned as Java doubles
        Assert.assertEquals(
                "1.0",
                json.format( JSPluginTest.executeTestScript( "1", (Map)parser.parse( "{}")) ) );
    }

    @Test
    public void testMultilineScriptWithLinebreaks()
    {
        Assert.assertEquals( "2.0",
                json.format( JSPluginTest.executeTestScript( "1;\n2", null) ) );
    }

    @Test
    public void testMultiThread()
    {
        for ( int i = 0; i < 250; i++ )
        {
            final int x = i;
            
            new Thread()
            {
                public void run()
                {
                	// Returning values with "return" evidently doesn't work right in Rhino -- find out why.
                	String script = "x=" + x + ";" +
                					"x";
                	Representation rep = JSPluginTest.executeTestScript( script , null);
                	String out = json.format( rep );
                	System.out.print(out);
                    Assert.assertEquals(x + "", out );
                }
            }.start();
        }
    }

    private static Representation executeTestScript(final String script, Map params)
    {
        Transaction tx = null;
        try
        {
            tx = neo4j.beginTx();
            return plugin.executeScript( neo4j, script, params );
        }
        finally
        {
            tx.success();
            tx.finish();
        }
    }

    @Test
    public void testExecuteScriptGetVerticesBySpecifiedName() throws Exception
    {
    	//String script = "g.V.filter(){it.name=='marko'}.next()";
    	String script = "pipe.start(g).V().filter(new PipeFunction<Vertex,Boolean>() {" +
                        "  public Boolean compute(Vertex argument) {" +
                        "    argument.getProperty('name') == 'marko';" +
                        "  }" +
                        "}).next()";
  
    	String out = json.format( JSPluginTest.executeTestScript( script, null) );
    	System.out.println(out);
    	
        JSONObject object = (JSONObject) parser.parse( out );
        Assert.assertEquals(
                ( (JSONObject) object.get( "data" ) ).get( "name" ), "marko" );
        Assert.assertEquals(
                ( (JSONObject) object.get( "data" ) ).get( "age" ), 29l );
        String self = (String) ( (JSONObject) object ).get( "self" );
        Assert.assertEquals( self.substring( self.lastIndexOf( "/" ) + 1 ), "1" );
    }
}
