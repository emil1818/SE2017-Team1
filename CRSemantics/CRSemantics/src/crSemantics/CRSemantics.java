package crSemantics;

import java.util.ArrayList;
import java.util.List;

import graph.ConresActivity;
import graph.ConresGraph;
import graph.ConresRelation;
import graph.Type;
import interfaces.Graph;
import interfaces.Semantics;
import utils.ExceptionTags;
import utils.SemanticsFactory;



//TODO MAKE A DEEP COPY OF THE CRGRAPH WHEN RECIEVED
//TODO IF TIME ADD MEMORIZATION TO THE HELPER FUNCTIONS.

public class CRSemantics implements Semantics {

	public SemanticsFactory semanticsFactory = null;

	public CRSemantics(){
		this.semanticsFactory = new SemanticsFactory();
	}


    public boolean isExecutable(ConresGraph graph, int id) {
        for(int i = 0; i < graph.activities.size(); i++) {
            if(graph.activities.get(i).id == id)
                // what if multiple ConresActivities have the same id?
                // not our problem
                return isExecutable(graph, graph.activities.get(i));
        }
        return true;
    }

    public boolean isExecutable(ConresGraph graph, ConresActivity activity) {
        for(int i = 0; i < graph.relations.size(); i++) {
            if(graph.relations.get(i).type == Type.CONDITION)
                // this definitely needs to be tested. object equivalence
                if (graph.relations.get(i).child == activity)
                    if(graph.relations.get(i).parent.isPending || !graph.relations.get(i).parent.isExecuted)
                        return false;
        }
        return true;
    }

    @Override
    public List<Integer> getPossibleActions(Graph graph) throws Exception {
    	ConresGraph crGraph = null;
    	try{
    		crGraph = (ConresGraph)graph;
    	}catch(Exception e){
    		throw new Exception("This is not a CRGraph");
    	}
        List<Integer> actions = new ArrayList<Integer>();
        for(int i = 0; i < crGraph.activities.size(); i++) {
            if(!crGraph.activities.get(i).isExecuted && noBlockingConditions(crGraph.activities.get(i), crGraph)){
            	actions.add(crGraph.activities.get(i).id);
            }
        }
        return actions;
    }

    /**
     * Doing it this way, it's possible that no events are executed
     * Maybe there should be a check if all events are in our graph
     */

    @Override
    public ConresGraph executeAction(Graph graph, List<Integer> ids) throws Exception {
    	ConresGraph crGraph = null;
    	try{
    		crGraph = (ConresGraph)graph;
    	}catch(Exception e){
    		throw new Exception("This is not a CRGraph");
    	}
    	
    	if(ids.isEmpty())
    		throw new Exception(ExceptionTags.EmptyListException.toString());
    	
    	// Its nested graph activities that needs execution
    	if(ids.size() > 1){
    		for(int i = 0; i < crGraph.activities.size(); i++){
    			if(crGraph.activities.get(i).id == ids.get(0)){
    				
    				//TODO check all condition relations
    				ConresActivity activity = crGraph.activities.get(i);
    				if (!noBlockingConditions(activity, crGraph)){
    					throw new Exception("Blocking condition relation");
    				}
    				
    				//Check that is has a nested Graph

    				if(activity.nestedGraph == null){
    					throw new Exception("There is no nested graph for given id");
    				}
    				
    				Semantics semantics = semanticsFactory.getSemantics(activity.nestedGraph);
    				
    				ids.remove(0);
    				
    				activity.nestedGraph = semantics.executeAction(activity.nestedGraph, ids);
    				
    			}
    		}
    		
        }
    	
    	// Its activity in this graph that needs execution
    	if (ids.size() == 1){
    		for (int i = 0; i < crGraph.activities.size(); i++){
    			if(crGraph.activities.get(i).id == ids.get(0)){
    				ConresActivity activity = crGraph.activities.get(i);
    				activity.isExecuted = true;
    				activity.isPending = false;
    				
    				//Check all condition relations
    				if(!noBlockingConditions(activity, crGraph)){
    					throw new Exception(ExceptionTags.InvalidActionException.toString());
    				}  				
    				
    				//Check if it makes anything pending, and mark them as pending  				
    				makeActivitiesPending(activity, crGraph);
    				
    			}
    		}
    	}
        return crGraph;
    }
    
    // Function used to Response relations 
    private void makeActivitiesPending(ConresActivity activity, ConresGraph crGraph) {
    	for (int i = 0; i < crGraph.relations.size(); i++){
    		ConresRelation relation = crGraph.relations.get(i);
    		if(relation.parent.id == activity.id && relation.type == Type.RESPONSE){
    			relation.child.isPending = true;
    		}
    	}	
	}

    // Function to check for any not done conditions
	private boolean noBlockingConditions(ConresActivity activity, ConresGraph crGraph) {
		for(int i = 0; i < crGraph.relations.size(); i++){
			ConresRelation relation = crGraph.relations.get(i);
			if(relation.child.id == activity.id && relation.type == Type.CONDITION && !relation.parent.isExecuted){
				return false;
			}
			else if(relation.child.id == activity.id && relation.type == Type.CONDITION && relation.parent.isExecuted){
				
				if(!noBlockingConditions(relation.parent, crGraph)){
					return false;
				}
			}
		}
		return true;
	}


	@Override
    public boolean isFinished(Graph graph) throws Exception {
    	ConresGraph crGraph = null;
    	try{
    		crGraph = (ConresGraph)graph;
    	}catch(Exception e){
    		throw new Exception(ExceptionTags.InvalidActionException.toString());
    	}

        for(int i = 0; i < crGraph.activities.size(); i++){

        	ConresActivity activity = crGraph.activities.get(i);

        	if(activity.isPending)
        		return false;

        	if (activity.nestedGraph != null){
        		//Check if nested graph is done
        		Graph nestedGraph = activity.nestedGraph;
            	Semantics semantics = semanticsFactory.getSemantics(nestedGraph);

            	if(!semantics.isFinished(nestedGraph))
            		return false;
        	}
        }
        return true;
    }
}
