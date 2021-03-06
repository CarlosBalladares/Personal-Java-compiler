package analysis.implementation;

import codegen.assem.Instr;
import ir.temp.Temp;

import java.util.Collections;
import java.util.HashMap;

import translate.implementation.TRExp;
import util.List;

import analysis.FlowGraph;
import analysis.Liveness;
import analysis.util.ActiveSet;
import analysis.util.graph.Node;


public class LivenessImplementation<N> extends Liveness<N> {

    private HashMap<Integer, Pair<ActiveSet<Temp>, ActiveSet<Temp>>> livenessMap;

    public LivenessImplementation(FlowGraph<N> graph) {
        super(graph);
        getSets();

    }

    public void getSets(){
        livenessMap= new HashMap<>();

        for(Node<N> node: super.g.nodes()){
            livenessMap.put(node.getKey(), new Pair(new ActiveSet<Temp>(), new ActiveSet<Temp>()));
        }

        for (Node<N> node: super.g.nodes()){
            Instr n= (Instr) node.wrappee();
            Pair<ActiveSet<Temp>, ActiveSet<Temp>> sets=livenessMap.get(node.getKey());
            ActiveSet<Temp> in=sets.first;
            ActiveSet<Temp> out=sets.second;


            out.addListener(new ActiveSet.ASListener<Temp>() {
                @Override
                public void elementAdded(Temp temp) {
                    if(!n.def().contains(temp)){
                        in.add(temp);
                    }
                }
            });

            List<Node<N>> succs= node.succ();

            for (Node<N> succ : succs) {
                ActiveSet<Temp> succIn=livenessMap.get(succ.getKey()).first;
                succIn.addListener(new ActiveSet.ASListener<Temp>() {
                    @Override
                    public void elementAdded(Temp temp) {
                        out.add(temp);
                    }
                });

            }
        }

        for(Node<N> node : super.g.nodes()){
            Pair<ActiveSet<Temp>, ActiveSet<Temp>> sets=livenessMap.get(node.getKey());
            Instr n= (Instr) node.wrappee();
            sets.first.addAll(n.use());
        }
    }

    @Override
    public List<Temp> liveOut(Node<N> node) {
        // This dummy implementation says that nothing is live

        //ActiveSet<Integer> s = new ActiveSet<>();

        return livenessMap.get(node.getKey()).second.getElements();
    }

    private List<Temp> liveIn(Node<N> node) {
        // This dummy implementation says that nothing is live
        return livenessMap.get(node.getKey()).first.getElements();

    }

    private String shortList(List<Temp> l) {
        java.util.List<String> reall = new java.util.ArrayList<String>();
        for (Temp t : l) {
            reall.add(t.toString());
        }
        Collections.sort(reall);
        StringBuffer sb = new StringBuffer();
        sb.append(reall);
        return sb.toString();
    }

    private String dotLabel(Node<N> n) {
        StringBuffer sb = new StringBuffer();
        sb.append(shortList(liveIn(n)));
        sb.append("\\n");
        sb.append(n);
        sb.append(": ");
        sb.append(n.wrappee());
        sb.append("\\n");
        sb.append(shortList(liveOut(n)));
        return sb.toString();
    }

    private double fontSize() {
        return (Math.max(30, Math.sqrt(Math.sqrt(g.nodes().size() + 1)) * g.nodes().size() * 1.2));
    }

    private double lineWidth() {
        return (Math.max(3.0, Math.sqrt(g.nodes().size() + 1) * 1.4));
    }

    private double arrowSize() {
        return Math.max(2.0, Math.sqrt(Math.sqrt(g.nodes().size() + 1)));
    }

    @Override
    public String dotString(String name) {
        StringBuffer out = new StringBuffer();
        out.append("digraph \"Flow graph\" {\n");
        out.append("labelloc=\"t\";\n");
        out.append("fontsize=" + fontSize() + ";\n");
        out.append("label=\"" + name + "\";\n");

        out.append("  graph [size=\"6.5, 9\", ratio=fill];\n");
        for (Node<N> n : g.nodes()) {
            out.append("  \"" + dotLabel(n) + "\" [fontsize=" + fontSize());
            out.append(", style=\"setlinewidth(" + lineWidth() + ")\", color=" + (g.isMove(n) ? "green" : "blue"));
            out.append("]\n");
        }
        for (Node<N> n : g.nodes()) {
            for (Node<N> o : n.succ()) {
                out.append("  \"" + dotLabel(n) + "\" -> \"" + dotLabel(o) + "\" [arrowhead = normal, arrowsize=" + arrowSize() + ", style=\"setlinewidth(" + lineWidth() + ")\"];\n");
            }
        }

        out.append("}\n");
        return out.toString();
    }

}
