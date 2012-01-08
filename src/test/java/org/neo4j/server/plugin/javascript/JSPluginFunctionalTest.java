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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.helpers.Pair;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.rest.AbstractRestFunctionalTestBase;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.GraphDescription.NODE;
import org.neo4j.test.GraphDescription.PROP;
import org.neo4j.test.GraphDescription.PropType;
import org.neo4j.test.GraphDescription.REL;
import org.neo4j.test.TestData.Title;
import org.neo4j.visualization.asciidoc.AsciidocHelper;

import com.google.gson.Gson;

public class JSPluginFunctionalTest extends AbstractRestFunctionalTestBase
{
    private static final String ENDPOINT = "http://localhost:7474/db/data/ext/JSPlugin/graphdb/execute_script";
    
    private String doRestCall( String script, Status status, Pair<String, String>... params )
    {
        return doJavascriptRestCall( ENDPOINT, script, status, params );
    }
    
    protected String doJavascriptRestCall( String endpoint, String script, Status status, Pair<String, String>... params ) {
        data.get();
        String parameterString = createParameterString( params );


        String queryString = "{\"script\": \"" + createScript( script ) + "\"," + parameterString+"},"  ;

        gen.get().expectedStatus( status.getStatusCode() ).payload(
                queryString ).description(formatJavaScript( createScript( script ) ) );
        return gen.get().post( endpoint ).entity();
    }
    /**
     * Scripts can be sent as URL-encoded In this example, the graph has been
     * autoindexed by Neo4j, so we can look up the name property on nodes.
     */
    @Test
    @Title( "Send a Gremlin Script - URL encoded" )
    @Documented
    @Graph( value = { "I know you" }, autoIndexNodes = true )
    public void testGremlinPostURLEncoded() throws UnsupportedEncodingException
    {
        data.get();
        //String script = "g.idx('node_auto_index')[[name:'I']].out";
    	        
        String script = "importPackage(Packages.com.tinkerpop.blueprints.pgm.impls.neo4j);" +
        				"var index = gdb.index().forNodes('node_auto_index');" +
        				"var node = index.get('name','I').getSingle();" +
    					"var vertex = Neo4jVertex(node,g);" +
						"pipe.start(vertex).out([]);";
        
        //HashMap map = new java.util.HashMap();
        
        String payload =  "script=" + URLEncoder.encode( script, "UTF-8" );
        MediaType payloadType = MediaType.APPLICATION_FORM_URLENCODED_TYPE;
        
        gen().expectedStatus( Status.OK.getStatusCode() ).description( formatGroovy( script ) );
        String response = gen().payload(payload).payloadType(payloadType).post( ENDPOINT ).entity();   
        
        //System.out.print(response);
        assertTrue( response.contains( "you" ) );
    }

    /**
     * Send a Gremlin Script, URL-encoded with UTF-8 encoding, with additional
     * parameters.
     */
    // evidently URL params isn't supported -- only JSON?
    // yes, let's skip URLencoded entirely, MAP parameters get messy. PN
    @Title( "Send a Gremlin Script with variables in a JSON Map - URL encoded" )
    @Documented
    @Graph( value = { "I know you" } )
    public void testGremlinPostWithVariablesURLEncoded()
            throws UnsupportedEncodingException
    {
    	data.get();
        //final String script = "g.v(me).out;";
    	final String script = "pipe.start(g.getVertex(me)).out()";
        final String params = "{\"me\":" + data.get().get( "I" ).getId() + "}";

        
        gen().description( formatGroovy( script ) );
        String response = gen().expectedStatus( Status.OK.getStatusCode() ).payload(
                "script=" + URLEncoder.encode( script, "UTF-8" ) + "&params="
                        + URLEncoder.encode( params, "UTF-8" ) )

        .payloadType( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post(
                ENDPOINT ).entity();
        assertTrue( response.contains( "you" ) );
    }

    /**
     * Send a Gremlin Script, as JSON payload and additional parameters
     */
    @Test
    @Title( "Send a Gremlin Script with variables in a JSON Map" )
    @Documented
    @Graph( value = { "I know you" } )
    public void testGremlinPostWithVariablesAsJson()
            throws UnsupportedEncodingException
    {
    	//final String script = "g.v(me).out";
    	String script = "pipe.start(g.getVertex(me)).out([]);";
    	Pair<String, String> params = Pair.of( "me", data.get().get( "I" ).getId() + "");
    	
        String response = doRestCall( script, Status.OK, params);
        assertTrue( response.contains( "you" ) );
    }
    
    
    /**
     * Import a graph form a http://graphml.graphdrawing.org/[GraphML] file can
     * be achieved through the Gremlin GraphMLReader. The following script
     * imports a small GraphML file from an URL into Neo4j, resulting in the
     * depicted graph. It then returns a list of all nodes in the graph.
     */
    @Test
    @Documented
    @Title( "Load a sample graph" )
    public void testGremlinImportGraph() throws UnsupportedEncodingException
    {
        //String script = "g.loadGraphML('https://raw.github.com/neo4j/gremlin-plugin/master/src/data/graphml1.xml');" +
        //                "g.V;";
        String script = "importPackage(Packages.com.tinkerpop.blueprints.pgm.util.graphml);" +
        				"url = 'https://raw.github.com/neo4j/gremlin-plugin/master/src/data/graphml1.xml';" +
        				"GraphMLReader.inputGraph(g, new java.net.URL(url).openStream());" +
        				"pipe.start(g).V()";       	
        String response = doRestCall( script, Status.OK );
        System.out.println(response);
        assertTrue( response.contains( "you" ) );
        assertTrue( response.contains( "him" ) );
    }

    /**
     * Exporting a graph can be done by simple emitting the appropriate String.
     */
    @Test
    @Documented
    @Title( "Emit a sample graph" )
    @Graph( value = { "I know you", "I know him" } )
    public void emitGraph() throws UnsupportedEncodingException
    {
        String script = "importPackage(Packages.com.tinkerpop.blueprints.pgm.util.graphml);" +
        				"writer = new GraphMLWriter(g);" +
        				"out = new java.io.ByteArrayOutputStream();" +
                        "writer.outputGraph(out);" +
                        "result = out.toString();"; 
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "graphml" ) );
        assertTrue( response.contains( "you" ) );
    }
    
    /**
     * To set variables in the bindings for the Gremlin Script Engine on the
     * server, you can include a +params+ parameter with a String representing a
     * JSON map of variables to set to initial values. These can then be
     * accessed as normal variables within the script.
     */
    @Test
    @Documented
    @Title( "Set script variables" )
    public void setVariables() throws UnsupportedEncodingException
    {
        String script = "meaning_of_life";

        Map<String, Object> params = new HashMap<String, Object>();        
        params.put("meaning_of_life", 42.0);

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("script",script);
        payload.put("params",params);
        
        description( formatGroovy( script ) );
        String response = gen()
        					.expectedStatus( Status.OK.getStatusCode() )
        					.payload( new Gson().toJson( payload ) )
        					.payloadType( MediaType.APPLICATION_JSON_TYPE )
        					.post( ENDPOINT )
        					.entity();
        assertTrue( response.contains( "42.0" ) );
    }

    /**
     * The following script returns a sorted list of all nodes connected via
     * outgoing relationships to node 1, sorted by their `name`-property.
     */
    @Test
    @Documented
    @Title( "Sort a result using raw Groovy operations" )
    @Graph( value = { "I know you", "I know him" }, autoIndexNodes = true )
    public void testSortResults() throws UnsupportedEncodingException
    {
        // String script = "g.idx('node_auto_index')[[name:'I']].out.sort{it.name}";

        // NOTE: this sort assertion has been commented out -- still looking for a clean way to do sorts
        String script = "importPackage(Packages.com.tinkerpop.blueprints.pgm.impls.neo4j.util);" +
        				"index = gdb.index().forNodes('node_auto_index');" +
        				"ids = new java.util.ArrayList();" +
        				"hits = index.get('name','I');" +
        				"sequence = Neo4jVertexSequence(hits,g);" +
						"nodesObj = pipe.start(sequence).out([]);";
        
						//"var nodesArray = [];" +
						//"while (nodesObj.hasNext()) { nodesArray.push(nodesObj.next()); }; " +
						//"function nameSort(a,b) {return a.name-b.name;};" +
						//"nodesArray.sort(nameSort);";
        
        String response = doRestCall( script, Status.OK );
        //System.out.println(response);
        assertTrue( response.contains( "you" ) );
        assertTrue( response.contains( "him" ) );
        //assertTrue( response.indexOf( "you" ) > response.indexOf( "him" ) );
    }

    /**
     * The following script returns paths. Paths in Gremlin consist of the pipes
     * that make up the path from the starting pipes. The server is returning
     * JSON representations of their content as a nested list.
     */
    @Test
    @Title( "Return paths from a Gremlin script" )
    @Documented
    @Graph( value = { "I know you", "I know him" } )
    public void testScriptWithPaths()
    {
        //String script = "g.v(%I%).out.name.paths";
        String script = "pipe.start(g.getVertex(%I%)).out([]).property('name').path([])";
        String response = doRestCall( script, Status.OK );
        //System.out.print(response);
        assertTrue( response.contains( "you" ) );
        assertTrue( response.contains( "him" ) );
    }

    @Test
    public void testLineBreaks() throws UnsupportedEncodingException
    {
        // be aware that the string is parsed in Java before hitting the wire,
        // so escape the backslash once in order to get \n on the wire.
        String script = "1\\n2";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "2" ) );
    }

    /**
     * To send a Script JSON encoded, set the payload Content-Type Header. In
     * this example, find all the things that my friends like, and return a
     * table listing my friends by their name, and the names of the things they
     * like in a table with two columns, ignoring the third named step variable
     * +I+. Remember that everything in Gremlin is an iterator - in order to
     * populate the result table +t+, iterate through the pipes with
     * +iterate()+.
     */
    @Test
    @Title( "Send a Gremlin Script - JSON encoded with table results" )
    @Documented
    @Graph( value = { "I know Joe", "I like cats", "Joe like cats",
            "Joe like dogs" } )
    public void testGremlinPostJSONWithTableResult()
    {
    	//String script = "t = new Table();" +
    	//				"pipe.start(g.getVertex(%I%)).as('I').out('know').as('friend').out('like').as('likes').table(t,['friend','likes']){it.name}{it.name}.iterate();" +
    	//				"return t";
    	
    	String script = "importPackage(Packages.com.tinkerpop.pipes.util);" +
        				"t = new Table();" +
        				"stepNames = new java.util.ArrayList();" +
        				"stepNames.add('friend');" +
        				"stepNames.add('likes');" + 
        				"columnFunc = function(vertex) { return vertex.getProperty('name'); };" +
                        "pipe.start(g.getVertex(%I%)).as('I')." +
                        "out(['know']).as('friend')." +
                        "out(['like']).as('likes')." +
            			"table(t,stepNames,[columnFunc,columnFunc]).iterate();" +
                        "t;";

        String response = doRestCall( script, Status.OK );
        //System.out.print(response);
        assertTrue( response.contains( "cats" ) );
    }

    /**
     * When returning an iterable nested result like a pipe, the data will be
     * serialized according to the recursive resolution of the pipe elements, as
     * shown below.
     */
    @Test
    @Graph( value = { "I know Joe", "I like cats", "Joe like cats",
            "Joe like dogs" } )
    public void returning_nested_pipes()
    {
        String script = "importPackage(Packages.com.tinkerpop.pipes.util);" +
        				"columnFunc = function(vertex) { return vertex.getProperty('name'); };" +
        				"pipe.start(g.getVertex(%I%)).as('I')." +
        				"out(['know']).as('friend')." +
        				"out(['like']).as('likes')." +
        				"table(new Table(),[columnFunc,columnFunc]).cap();";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "cats" ) );
    }

    /**
     * Send an arbitrary Groovy script - Lucene sorting.
     * 
     * This example demonstrates that you via the Groovy runtime embedded with
     * the server have full access to all of the servers Java APIs. The below
     * example creates Nodes in the database both via the Blueprints and the
     * Neo4j API indexes the nodes via the native Neo4j Indexing API constructs
     * a custom Lucene sorting and searching returns a Neo4j IndexHits result
     * iterator.
     */
    @Test
    @Documented
    @Graph( value = { "I know Joe", "I like cats", "Joe like cats",
            "Joe like dogs" } )
    public void sendArbtiraryGroovy()
    {
        String script = "importPackage(Packages.org.neo4j.graphdb.index);" +
                		"importPackage(Packages.org.neo4j.index.lucene);" +
                		"importPackage(Packages.org.apache.lucene.search);" +
                		"neo4j = g.getRawGraph();" +
                		"tx = neo4j.beginTx();" +
                		"meNode = neo4j.createNode();" +
                		"meNode.setProperty('name','me');" +
                        "youNode = neo4j.createNode();" +
                        "youNode.setProperty('name','you');" +
                        "idxManager = neo4j.index();" +
                        "personIndex = idxManager.forNodes('persons');" +
                        "personIndex.add(meNode,'name',meNode.name);" +
                        "personIndex.add(youNode,'name',youNode.getProperty('name'));" +
                		"tx.success();" +
                		"tx.finish();" +
                        "query = new QueryContext( 'name:*' ).sort( new Sort(new SortField( 'name',SortField.STRING, true ) ) );" +	                     
                        "results = personIndex.query( query );" ;                
        String response = doRestCall( script, Status.OK );
        System.out.println(response);
        assertTrue( response.contains( "me" ) );

    }

    /**
     * Imagine a user being part of different groups. A group can have different
     * roles, and a user can be part of different groups. He also can have
     * different roles in different groups apart from the membership. The
     * association of a User, a Group and a Role can be referred to as a
     * _HyperEdge_. However, it can be easily modeled in a property graph as a
     * node that captures this n-ary relationship, as depicted below in the
     * +U1G2R1+ node.
     * 
     * To find out in what roles a user is for a particular groups (here
     * 'Group2'), the following script can traverse this HyperEdge node and
     * provide answers.
     */
    @Test
    @Title( "HyperEdges - find user roles in groups" )
    @Documented
    @Graph( value = { "User1 in Group1", "User1 in Group2",
            "Group2 canHave Role2", "Group2 canHave Role1",
            "Group1 canHave Role1", "Group1 canHave Role2", "Group1 isA Group",
            "Group2 isA Group", "Role1 isA Role", "Role2 isA Role",
            "User1 hasRoleInGroup U1G2R1", "U1G2R1 hasRole Role1",
            "U1G2R1 hasGroup Group2", "User1 hasRoleInGroup U1G1R2",
            "U1G1R2 hasRole Role2", "U1G1R2 hasGroup Group1" } )
    public void findGroups()
    {
        String script = "filterFunc = function(vertex) { return vertex.getProperty('name') == 'Group2' };" +
        				"pipe.start(g.getVertex(%User1%))" +
        				".out(['hasRoleInGroup']).as('hyperedge')" +
        				".out(['hasGroup']).filter(filterFunc)" +
        				".back('hyperedge').out(['hasRole']).property('name')";
        String response = doRestCall( script, Status.OK );
        //System.out.print(response);
        assertTrue( response.contains( "Role1" ) );
        assertFalse( response.contains( "Role2" ) );
    }

    /**
     * This example is showing a group count in Germlin, for instance the
     * counting of the different relationship types connected to some the start
     * node. The result is collected into a variable that then is returned.
     */
    @Test
    @Documented
    @Graph( { "Peter knows Ian", "Ian knows Peter", "Peter likes Bikes" } )
    public void group_count() throws UnsupportedEncodingException, Exception
    {
    	String script = "m = new java.util.HashMap();" +
                		"pipe.start(g.getVertex(%Peter%)).bothE([]).label().groupCount(m).iterate();" +
                		"m";               
        String response = doRestCall( script, Status.OK );
       // System.out.println(response);
        assertTrue( response.contains( "knows=2" ) );
    }

    /**
     * This example is showing a group count in Gremlin, for instance the
     * counting of the different relationship types connected to some the start
     * node. The result is collected into a variable that then is returned.
     * 
     * @@graph1
     */
    //@Ignore // until we have an each() step
    @Test
    @Documented
    @Graph( { "Peter knows Ian", "Ian knows Peter", "Peter likes Bikes" } )
    public void modify_the_graph_while_traversing()
            throws UnsupportedEncodingException, Exception
    {
        String name = "starting_graph" + gen.get().getTitle();
        String graphViz = AsciidocHelper.createGraphViz( "Starting Graph", graphdb(), name );
        gen().addSnippet( "graph1", graphViz);
        
        assertTrue( getNode( "Peter" ).hasRelationship() );
        
        // String script = "g.v(%Peter%).bothE.each{g.removeEdge(it)}";
        String script = "removeFunc = function(edge) { g.removeEdge(edge); };" +
        				"pipe.start(g.getVertex(%Peter%)).bothE([]).transform(removeFunc);";
        String response = doRestCall( script, Status.OK );
        //System.out.println(response);
        assertFalse( getNode( "Peter" ).hasRelationship() );
    }

    /**
     * Multiple traversals can be combined into a single result, using splitting
     * and merging pipes in a lazy fashion.
     */
    @Test
    @Documented
    @Graph( value = { "Peter knows Ian", "Ian knows Peter", "Marie likes Peter" }, autoIndexNodes = true )
    public void collect_multiple_traversal_results()
            throws UnsupportedEncodingException, Exception
    {
        // String script = "g.idx('node_auto_index')[[name:'Peter']].copySplit(_().out('knows'), _().in('likes')).fairMerge.name";
    	
    	// using inE().outV() instead of in() because "in" is a reserved word in JavaScript
    	String script =	"importPackage(Packages.com.tinkerpop.gremlin.java);" +
    					"importPackage(Packages.com.tinkerpop.pipes.transform);" +
    					"importPackage(Packages.com.tinkerpop.blueprints.pgm.impls.neo4j);" +
    					"_ = function(vertex) { return GremlinPipeline().start(vertex); } ;" +
    					"var index = gdb.index().forNodes('node_auto_index');" +
    					"var node = index.get('name','Peter').getSingle();" +
    					"var vertex = Neo4jVertex(node,g);" +
    					"pipe.start(vertex)" +    	
    					".copySplit([_().out(['knows']),_().inE(['likes']).outV()])" +
    					".fairMerge().property('name');";	
    	
        String response = doRestCall( script, Status.OK );
        //System.out.println(response);
        assertTrue( response.contains( "Marie" ) );
        assertTrue( response.contains( "Ian" ) );
    }

    @Test
    public void getExtension()
    {
        String entity = gen.get().expectedStatus( Status.OK.getStatusCode() ).get(ENDPOINT ).entity();
        assertTrue( entity.contains( "map" ) );

    }

    /**
     * In order to return only certain sections of a Gremlin result, you can use
     * native Groovy contstructs like +drop()+ and +take()+ to skip and chunk
     * the result set. However, these are not lazy and will build up memory. It
     * is better to use the Gremlin +[start..end]+ pipe instead, providing a
     * lazy way for doing this.
     * 
     * Also, note the use of the +filter{}+ closure to filter nodes.
     */
    @Test
    @Graph( value = { "George knows Sara", "George knows Ian" }, autoIndexNodes = true )
    public void chunking_and_offsetting_in_Gremlin()
            throws UnsupportedEncodingException
    {
    	    	
        // String script = "g.v(%George%).out('knows').filter{it.name == 'Sara'}[0..100]";
        String script = "filterFunc = function(vertex) { return vertex.getProperty('name') == 'Sara'; };" +
        				"pipe.start(g.getVertex(%George%)).out(['knows']).filter(filterFunc).next(100)"; // [0..100]
        String response = doRestCall( script, Status.OK );
        //System.out.print(response);
        assertTrue( response.contains( "Sara" ) );
        assertFalse( response.contains( "Ian" ) );
    }

    /**
     * Of course, Neo4j primitives liek +Nodes+, +Relationships+ and
     * +GraphDatabaseService+ are returned as Neo4j REST entities
     */
    @Test
    @Graph( value = { "George knows Sara", "George knows Ian" }, autoIndexNodes = true )
    public void returning_Neo4j_primitives()
            throws UnsupportedEncodingException
    {
        //String script = "g.getRawGraph()";
        String script = "transformFunc = function(graph) { return graph.getRawGraph(); };" +
        				"pipe.start(g).transform(transformFunc).next()"; 
        String response = doRestCall( script, Status.OK );
        //System.out.print(response);
        assertTrue( response.contains( "neo4j_version" ) );
    }

    /**
     * 
     */
    @Test
    @Graph( value = { "George knows Sara", "George knows Ian" }, autoIndexNodes = true )
    public void returning_paths() throws UnsupportedEncodingException
    {
        //String script = "g.v(%George%).out().paths()";
    	String script = "pipe.start(g.getVertex(%George%)).out([]).path([])";
        String response = doRestCall( script, Status.OK );
        //System.out.println(response);
        assertTrue( response.contains( "Ian" ) );
        assertTrue( response.contains( "Sara" ) );
    }

    /**
     * This example demonstrates basic collaborative filtering - ordering a
     * traversal after occurence counts and substracting objects that are not
     * interesting in the final result.
     * 
     * Here, we are finding Friends-of-Friends that are not Joes friends
     * already. The same can be applied to graphs of users that +LIKE+ things
     * and others.
     */
    @Documented
    @Test
    @Graph( value = { "Joe knows Bill", "Joe knows Sara", "Sara knows Bill",
            "Sara knows Ian", "Bill knows Derrick", "Bill knows Ian",
            "Sara knows Jill" }, autoIndexNodes = true )
    public void collaborative_filtering() throws UnsupportedEncodingException
    {
        //String script = "x=[];fof=[:];"
        //                + "g.v(%Joe%).out('knows').aggregate(x).out('knows').except(x).groupCount(fof).iterate();fof.sort{a,b -> b.value <=> a.value}";
        String script = "var x = new java.util.ArrayList();" +
        				"var fof = new java.util.HashMap();" +
        				"pipe.start(g.getVertex(%Joe%))." +
        				"out(['knows']).aggregate(x)." +
        				"out(['knows']).except(x).groupCount(fof).iterate();" +
        				"fof"; // sort: http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java/3420912
        			
        String response = doRestCall( script, Status.OK );
        System.out.println(response);
        assertFalse( response.contains( "v[" + data.get().get( "Bill" ).getId() ) );
        assertFalse( response.contains( "v[" + data.get().get( "Sara" ).getId() ) );
        assertTrue( response.contains( "v[" + data.get().get( "Ian" ).getId() ) );
        assertTrue( response.contains( "v[" + data.get().get( "Jill" ).getId() ) );
        assertTrue( response.contains( "v[" + data.get().get( "Derrick" ).getId() ) );
    }
    
    /**
     */
    @Documented
    @Test
    //@Ignore //preparation to track down a Gremlin issue
    @Graph( value = { 
            "Root AllFriends John", 
            "Root AllFriends Jack", 
            "Root AllFriends Jill", 
            "John HasPet ScoobieDoo",
            "Jack HasPet Garfield",
            "ScoobieDoo HasCareTaker Bob",
            "Garfield HasCareTaker Harry" }
    , autoIndexNodes = true )
    public void table_projections() throws UnsupportedEncodingException
    {
        data.get();
/*        String script = "g.v(%Root%).out('AllFriends').as('Friend').ifThenElse" +
        		"{it.out('HasPet').hasNext()}" +
        		"{it.out('HasPet')}" +
        		"{it}" +
        		".as('Pet').out('HasCareTaker').as('CareTaker').table(new Table()){it['name']}{it['name']}{it['name']}.cap";
 */
        String script = "importPackage(Packages.com.tinkerpop.pipes.util);" +
        				"ifFunc = function(vertex) { return vertex.getOutEdges(['HasPet']).hasNext(); };" +
        				"thenFunc = function(vertex) { return vertex.getOutEdges(['HasPet']).next().getInVertex(); };" +
        				"elseFunc = function(vertex) { return vertex; };" +
        				"columnFunc = function(vertex) { return vertex.getProperty('name'); };" + 
        				"pipe.start(g.getVertex(%Root%)).out(['AllFriends']).as('Friend')." +
        				"ifThenElse(ifFunc,thenFunc,elseFunc).as('Pet')." + 
        				"out(['HasCareTaker']).as('CareTaker')." +
        				"table(new Table(),[columnFunc,columnFunc,columnFunc]).cap();";
 
        String response = doRestCall( script, Status.OK );
        System.out.println(response);
    }

    /**
     * This is a basic stub example for implementing flow algorithms in for
     * instance http://en.wikipedia.org/wiki/Flow_network[Flow Networks] with a
     * pipes-based approach and scripting, here between +source+ and +sink+
     * using the +capacity+ property on relationships as the base for the flow
     * function and modifying the graph during calculation.
     * 
     * @@graph1
     */
    @Ignore
    @Documented 
    @Test
    @Graph( nodes = { @NODE( name = "source", setNameProperty = true ),
            @NODE( name = "middle", setNameProperty = true ),
            @NODE( name = "sink", setNameProperty = true ) }, relationships = {
            @REL( start = "source", end = "middle", type = "CONNECTED", properties = { @PROP( key = "capacity", value = "1", type = PropType.INTEGER ) } ),
            @REL( start = "middle", end = "sink", type = "CONNECTED", properties = { @PROP( key = "capacity", value = "3", type = PropType.INTEGER ) } ),
            @REL( start = "source", end = "sink", type = "CONNECTED", properties = { @PROP( key = "capacity", value = "1", type = PropType.INTEGER ) } ),
            @REL( start = "source", end = "sink", type = "CONNECTED", properties = { @PROP( key = "capacity", value = "2", type = PropType.INTEGER ) } ) }, autoIndexNodes = true )
    public void flow_algorithms_with_Gremlin()
            throws UnsupportedEncodingException
    {
    	
    	// See http://neo4j.org/nabble/#nabble-td3518678
    	
    	
//      String script = "source=pipe.start(g.v(%source%));" +
//		"sink=pipe.start(g.v(%sink%));" +
//		"maxFlow = 0;" +
//		"source.outE().inV().loop(2,new PipeFunction<Object,Boolean>() {" +
//		"  public Boolean compute(Object it) {" +
//		"    return !it.equals(sink);" +
//		"  }" +
//		"}).path().each{" +
//		"flow = it.capacity.min(); " +
//		"maxFlow += flow;" +
//		"it.findAll{it.capacity}.each{it.capacity -= flow}};" +
//		"return maxFlow;";
    	
        data.get();
        gen().addSnippet(
                "graph1",
                AsciidocHelper.createGraphViz( "Starting Graph", graphdb(),
                        "starting_graph" + gen.get().getTitle() ) );
                
/*        String script = "source=g.v(%source%);
 * 						+ "sink=g.v(%sink%);
 * 						+ "maxFlow = 0;"
                        + "source.outE.inV.loop(2){!it.object.equals(sink)}
                        + ".paths.each{"
                        + "	  flow = it.capacity.min(); "
                        + "   maxFlow += flow;"
                        + "	  it.findAll{it.capacity}.each{it.capacity -= flow}
                        + "};" 
                        + "maxFlow";*/
        
// 						"pathFunc = function(path) { flow = path.capacity.min(); maxFlow += flow; };" +
        
        String script = "var source = g.getVertex(%source%);" +
						"var sink = g.getVertex(%sink%);" +
						"var maxFlow = 0;" +
						"loopFunc = function(vertex) { return vertex.object.id != sink.id; };" +
						"capacityFunc = function(edge) { return edge.capacity; }; " +
						"sideEffectFunc = function(path) { " +
						"   capacities = path.object.map(capacityFunc).get(); " + 
						"	flow = Math.min.apply(Math,capacities); " +
						"	maxFlow += flow; " +
						"	for (var element in path) { " +
						"		if ( element.hasOwnProperty('capacity') ) { " +
						"			element.capacity -= flow; " +
						"		};" +
						"	};" +
						"   return true; " +
						"};" +
						"pipe.start(source).outE([]).inV().loop(2,loopFunc).path([]).sideEffect(sideEffectFunc);";
				
                		
//        				"//maxFlow;";
        
//        String script = "var source=pipe.start(g.getVertex(%source%));" +
//        				"var sink=pipe.start(g.getVertex(%sink%));" +
//        				"var maxFlow = 0;" +
//        				"loopFunc = function(vertex) { return vertex != sink };" +
//        				"pathFunc = function(path) { flow = path.capacity.min(); maxFlow += flow; };" +
//        				"transformFunc = function(path) { " +
//        				"	flow = path.capacity.min(); " +
//        				"	maxFlow += flow; " +
//        				"	while (paths.hasNext()) { if ( paths.next() }" +
//        				"	it.findAll{it.capacity}.each{it.capacity -= flow}; " +
//        				"};" +
//        				"source.out([]).loop(1,loopFunc).path([]);" +

       
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "4" ) );
    }

    // Ignored for now: use the Rhino engine's invokefunction() to reuse scripts between requests 
    // See http://docs.oracle.com/javase/6/docs/technotes/guides/scripting/programmer_guide/index.html
    @Ignore 
    @Test
    @Graph( value = { "Peter knows Ian", "Ian knows Peter", "Marie likes Peter" }, autoIndexNodes = true, autoIndexRelationships = true )
    public void test_Gremlin_load()
    {
        data.get();
/*        String script = "//neo4j = g.getRawGraph();" +
        		        "nodeIndex = g.idx('node_auto_index');" +
                        "edgeIndex = g.idx('relationship_auto_index');" +
                        "node = { uri, properties -> " +
                        "  existing = nodeIndex.get('uri', uri);" +
                        "  properties['uri'] = uri;" +
                        "  if (existing) {    " +
                        "    return existing[0];  " +
                        "  } else {" +
                        "    n = neo4j.createNode(); " +
                        "    for (entry in properties.entrySet()) {" +
                        "       n.setProperty(entry.key,entry.value); " +
                        "    };" +
                        "  };" +
                        "};" +
                        "Object.metaClass.makeNode = node;" +
                        "edge = { type, source_uri, target_uri, properties ->" +
                        "  source = nodeIndex.get('uri', source_uri).iterate();" +
                        "  target = nodeIndex.get('uri', target_uri).iterate();" +
                        "  nodeKey = source.id + '-' + target.id;" +
                        "  existing = edgeIndex.get('nodes', nodeKey);" +
                        "  if (existing) {" + 
                        "    return existing;" + 
                        "  };" +
                        "  properties['nodes'] = nodeKey;" +
                        "  r = source.createRelationshipTo(target,type);" +
                        "  for (entry in properties.entrySet()) {" +
                        "    r.setProperty(entry.key,entry.value);" + 
                        "  };" +
                        "};" + 
                        "Object.metaClass.makeEdge = edge;";*/
        
        
        String initScript = "importPackage(Packages.com.tinkerpop.blueprints.pgm);" +
        					"var neo4j = g.getRawGraph();" +
        					"var nodeIndex = gdb.index().forNodes('node_auto_index');" +
        					"var edgeIndex = gdb.index().forRelationships('relationship_auto_index');" +
        					"makeNode = function( uri, properties) {" +
        					"  var existing = nodeIndex.get('uri', uri);" +
        					"  if (existing.hasNext()) {    " +
        					"    return existing.next();  " +
        					"  }" +
        					"  g.setMaxBufferSize(0); " +
        					"  g.startTransaction(); " +
        					"  properties['uri'] = uri;" +
        					"  var node = neo4j.createNode(); " +
        					"  for (var key in properties) {" +
        					"    if (properties.hasOwnProperty(key)) {" +
        					"      node.setProperty(key,properties[key]);" +
        					"    };" + 
        					"  };" +
        					"  g.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);" +
        					"  return node;" + 
        					"};" +
        					"makeEdge = function(type, source_uri, target_uri, properties) {" +
        					"  var source = nodeIndex.get('uri', source_uri).iterate();" +
        					"  var target = nodeIndex.get('uri', target_uri).iterate();" +
        					"  var nodeKey = source.id + '-' + target.id;" +
        					"  var existing = edgeIndex.get('nodes', nodeKey);" +
        					"  if (existing.hasNext()) {" + 
        					"    return existing.next();" + 
        					"  };" +
        					"  g.setMaxBufferSize(0); " +
        					"  g.startTransaction(); " +
        					"  properties['nodes'] = nodeKey;" +
        					"  relationship = source.createRelationshipTo(target,type);" +
        					"  for (var key in properties) {" +
        					"    if (properties.hasOwnProperty(key)) {" +
        					"      relationship.setProperty(key,properties[key]);" +
        					"    };" + 
        					"  };" +
        					"  g.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);" +
        					"  return relationship;" +
        					"};";
          
        description( formatGroovy( initScript ) );
        
        Map<String, Object> initPayload = new HashMap<String, Object>();
        initPayload.put("script", initScript);
                
        String initResponse = gen.get()
        						 .expectedStatus( Status.OK.getStatusCode() )
        						 .payload(  new Gson().toJson( initPayload ) )
        						 .post( ENDPOINT )
        						 .entity();
        
        System.out.println(initResponse);
        
        for ( int i = 0; i < 1000; i++ )
        {
            String nodeUri = "uri" + i;
            String nodeScript = String.format("makeNode('%s',{});", nodeUri);
            
            Map<String, Object> nodePayload = new HashMap<String, Object>();
            nodePayload.put("script", nodeScript);
                        
            String nodeResponse = gen.get()
            						 .expectedStatus( Status.OK.getStatusCode() )
            						 .payload( new Gson().toJson( nodePayload ) )
            						 .post( ENDPOINT )
            						 .entity();
            
            System.out.println(nodeResponse);
            assertTrue( nodeResponse.contains( nodeUri ) );
        }
        
        for ( int i = 0; i < 999; i++ )
        {
        	String baseUri = "uri";
            String sourceUri = baseUri + i;
            String targetUri = baseUri + (i + 1);
            String edgeScript = String.format("makeEdge('knows','%s', '%s', {});", sourceUri, targetUri); 
            
            Map<String, Object> edgePayload = new HashMap<String, Object>();
            edgePayload.put("script", edgeScript);
            
            String edgeResponse = gen.get()
        							 .expectedStatus( Status.OK.getStatusCode() )
        							 .payload( new Gson().toJson( edgePayload ) )
        							 .post( ENDPOINT )
        							 .entity();
            System.out.println(edgeResponse);
            assertTrue( edgeResponse.contains( baseUri ) );
        }
    }
}
