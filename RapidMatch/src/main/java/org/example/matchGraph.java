package org.example;
import org.w3c.dom.ls.LSInput;

import java.io.*;
import java.util.*;


class Vertex_Q {
    int label;
    Set<Integer> nei;
    Map<Integer, List<Integer>> rep_nei;

    public Vertex_Q() {
        this.nei = new HashSet<>();
        this.rep_nei = new HashMap<>();
    }
}
class Vertex_G {
    int label;
    Set<Integer> nei;
    Map<Integer, Map<Integer, Set<Integer>>> cand;
    Map<Integer, Boolean> LI;

    public Vertex_G() {
        this.nei = new HashSet<>();
        this.cand = new HashMap<>();
        this.LI = new HashMap<>();
    }
}

class Split{
    List<Integer> core;
    List<Set<Integer>> core_nei;
    List<Integer> shell;
    List<Set<Integer>> shell_nei;
    Map<Integer, Set<Integer>> c_s_nei;
    public Split() {
        this.core = new ArrayList<>();
        this.core_nei = new ArrayList<>();
        this.shell = new ArrayList<>();
        this.shell_nei = new ArrayList<>();
        this.c_s_nei = new HashMap<>();
    }
}


public class matchGraph {
    public static int cnt = 0;
    public static List<Vertex_Q> Q = new ArrayList<>();
    public static List<Vertex_G> G = new ArrayList<>();
    public static int Q_size, G_size;
    public static List<Integer> update = new ArrayList<>();
    public static Map<Integer, List<Integer>> labels = new HashMap<>();
    public static Map<Integer, Map<Integer, Split>> matching_order = new HashMap<>();

    public static void inputQ(String qp) {
        try {
            Scanner qg = new Scanner(new File(qp));
            char c;
            int id, id1, id2, lb;

            while (qg.hasNext()) {
                c = qg.next().charAt(0);
                if (c == 'v') {
                    id = qg.nextInt();
                    lb = qg.nextInt();
                    if (lb == -1) {
                        lb = 0;
                    }
                    Vertex_Q u = new Vertex_Q();
                    u.label = lb;
                    Q.add(u);
                } else {
                    id1 = qg.nextInt();
                    id2 = qg.nextInt();
                    Q.get(id1).nei.add(id2);
                    Q.get(id2).nei.add(id1);
                    break;
                }
            }
            while (qg.hasNext()) {
                c = qg.next().charAt(0);
                id1 = qg.nextInt();
                id2 = qg.nextInt();
                Q.get(id1).nei.add(id2);
                Q.get(id2).nei.add(id1);
            }
            qg.close();
        } catch (FileNotFoundException e) {
            System.err.println("Fail to open query file.");
        }
        Q_size = Q.size();
        for (int ui = 0; ui < Q_size; ui++) {
            // key label val label对应的id
            if (!labels.containsKey(Q.get(ui).label)) {
                labels.put(Q.get(ui).label, new ArrayList<>());
            }
            labels.get(Q.get(ui).label).add(ui);
            //key nei_label val nei_id
            Map<Integer, List<Integer>> rep_nei = new HashMap<>();
            for (int uni: Q.get(ui).nei) {
                int uni_label = Q.get(uni).label;
                if (!rep_nei.containsKey(uni_label)) {
                    rep_nei.put(uni_label, new ArrayList<>());
                }
                rep_nei.get(uni_label).add(uni);
            }
            //存在多个相同标签的邻居结点才会存储在rep_nei
            for (Integer key: rep_nei.keySet()) {
                if (rep_nei.get(key).size() > 1) {
                    Q.get(ui).rep_nei.put(key, rep_nei.get(key));
                }
            }
        }
    }
    //验证结点是否存在于集合中
    public static boolean isInVec(int a, List<Integer> v) {
        for (int i = 0; i < v.size(); i++) {
            if (a == v.get(i)) {
                return true;
            }
        }
        return false;
    }
    public static void insertNei(Set<Integer> temp, int i, int ui, int uj, Split uj_second) {
        for (int nei: Q.get(i).nei) {
            int n = nei;
            if (n != ui && n != uj && !isInVec(n, uj_second.core) && !isInVec(n, uj_second.shell)) {
                temp.add(n);
            }
        }
    }

    public static boolean neiAllCore(int i, Split uj_second, int ui, int uj) {
        for (int nei: Q.get(i).nei) {
            int n = nei;
            if (n != ui && n != uj && !isInVec(n, uj_second.core)) {
                return false;
            }
        }
        return true;
    }

    //生成所有边的内核和外壳顶点
    public static void generateMO() {
        for (int ui = 0; ui < Q_size; ui++) {
            //key ui指向的边 val 内核和外壳顶点
            Map<Integer, Split> ui_first = new HashMap<>();
            for (int uj: Q.get(ui).nei) {
                if (uj >= 0) {
                    // ui->uj的内核外壳信息
                    Split uj_second = new Split();
                    Set<Integer> temp = new HashSet<>();
                    // 将节点ui和uj的邻居信息插入集合temp中
                    insertNei(temp, ui, ui, uj, uj_second);
                    insertNei(temp, uj, ui, uj, uj_second);


                    while (!temp.isEmpty()) {
                        //先查询是否存在外壳顶点
                        Set<Integer> temp2 = new HashSet<>(temp);
                        for (int ti: temp2) {
                            // 判断邻居是否都为内核顶点
                            if (neiAllCore(ti, uj_second, ui, uj)) {
//                                if (ui == 0 && uj == 2) {
//                                    System.out.println(123);
//                                }
                                Set<Integer> nei_info = new HashSet<>(Q.get(ti).nei);
                                //标记外壳顶点隔壁的核心顶点
                                int v_core = 0;
                                for (int i: uj_second.core) {
                                    if (nei_info.contains(i)) {
                                        v_core = i;
                                    }
                                }
                                // 将ti加入外壳顶点并从temp中移除
                                if (!uj_second.c_s_nei.containsKey(v_core)) {
                                    uj_second.c_s_nei.put(v_core, new HashSet());
                                }
                                uj_second.c_s_nei.get(v_core).add(uj_second.shell.size());
                                uj_second.shell.add(ti);
                                uj_second.shell_nei.add(nei_info);
                                temp.remove(ti);
                            }
                        }
                        // 查找核心顶点 找邻居结点最多的
                        if (!temp.isEmpty()) {
                            int nei_num = 0;
                            int core_v = 0;
                            for (int t: temp) {
                                Set<Integer> nei_noij = new HashSet<>(Q.get(t).nei);
                                nei_noij.remove(ui);
                                nei_noij.remove(uj);
                                if (nei_noij.size() > nei_num) {
                                    nei_num = nei_noij.size();
                                    core_v = t;
                                }
                            }
                            uj_second.core.add(core_v);
                            Set<Integer> nei_info = new HashSet<>();
                            for (int nei: Q.get(core_v).nei) {
                                int n = nei;
                                if (n == ui || n == uj || isInVec(n, uj_second.core)) {
                                    nei_info.add(nei);
                                }
                            }
                            uj_second.core_nei.add(nei_info);
                            insertNei(temp, core_v, ui, uj, uj_second);
                            temp.remove(core_v);
                        }
                    }
                    ui_first.put(uj, uj_second);
//                    System.out.println("ujsecond_info");
//                    System.out.println(ui + "  " + uj);
//                    System.out.println("uj_second.c_s_nei.size()" + uj_second.c_s_nei.size());

                }
            }
            matching_order.put(ui, ui_first);
        }
    }

    public static void inputG(String gp) {
        try {
            Scanner dg = new Scanner(new File(gp));
            char c;
            int id, id1, id2, lb;

            while (dg.hasNext()) {
                c = dg.next().charAt(0);
                if (c == 'v') {
                    Vertex_G v = new Vertex_G();
                    id = dg.nextInt();
                    lb = dg.nextInt();
                    if (labels.containsKey(lb)) {
                        v.label = lb;
                    } else {
                        v.label = -1;
                    }
                    G.add(v);
//                    System.out.println(v.label);
//                    System.out.println(id);
                } else {
                    G_size = G.size();
                    id1 = dg.nextInt();
                    id2 = dg.nextInt();
                    if (id1 < G_size && id2 < G_size && G.get(id1).label != -1 && G.get(id2).label != -1) {
                        G.get(id1).nei.add(id2);
                        G.get(id2).nei.add(id1);
                    }
                    break;
                }
            }

            while (dg.hasNext()) {
                c = dg.next().charAt(0);
                id1 = dg.nextInt();
                id2 = dg.nextInt();
                if (id1 < G_size && id2 < G_size && G.get(id1).label != -1 && G.get(id2).label != -1) {
                    G.get(id1).nei.add(id2);
                    G.get(id2).nei.add(id1);
                }
            }
            dg.close();
        } catch (FileNotFoundException e) {
            System.err.println("Fail to open data file.");
        }
    }


    public static void inputUpdate (String path) {
        update.clear();
        try {
            Scanner sg = new Scanner(new File(path));
            char c;
            int v1, v2, w;
            int cnt = 0;

            while (sg.hasNext()) {
                c = sg.next().charAt(0);
                v1 = sg.nextInt();
                v2 = sg.nextInt();
                update.add(v1);
                update.add(v2);
            }
            sg.close();
        } catch (FileNotFoundException e) {
            System.err.println("Fail to open s file.");
        }
    }

    public static void constructCand() {
        for (int vi = 0; vi < G_size; vi++) {
            int lb = G.get(vi).label;
//            System.out.println(lb);
            if (lb != -1) {
                for (int ui: labels.get(lb)) {
//                    System.out.print(lb);

                    //key vi_label对应的ui的邻居 val 对应ui邻居的vj的id
                    Map<Integer, Set<Integer>> ui_cand = new HashMap<>();
                    for (int vj: G.get(vi).nei) {

                        int vj_lb = G.get(vj).label;
                        for (int uj: Q.get(ui).nei) {
//                            System.out.println("vi:" + vi + " ui:" + ui + " vj:" + vj + " uj:" + uj);
                            if (!ui_cand.containsKey(uj)) {
                                ui_cand.put(uj, new HashSet<>());
                            }
                            if (uj >= 0 && vj >= 0 && Q.get(uj).label == vj_lb) {
                                ui_cand.get(uj).add(vj);
                            }
                        }

                    }
                    G.get(vi).cand.put(ui, ui_cand);
                    G.get(vi).LI.put(ui, true);
//                    System.out.println("vi:" + vi + "  " + "ui:" + ui);
//                    for (Map.Entry<Integer, Set<Integer>> map: G.get(vi).cand.get(ui).entrySet()) {
//                        System.out.print("uj:" + map.getKey() + "   vj:");
//                        for (int i: map.getValue()) {
//                            System.out.print(i + " ");
//                        }
//                        System.out.println();
//                    }
                }
            }
        }
    }

    public static boolean tryNei(int th, int vi, int ui, Set<Integer> used, List<Integer> to_check) {
        if (th == to_check.size()) {
            return true;
        } else {
            int uj = to_check.get(th);
            for (int vj: G.get(vi).cand.get(ui).get(uj)) {
                if (G.get(vj).label != Q.get(uj).label) {
                    G.get(vj).cand.remove(uj);
                    G.get(vj).LI.remove(uj);
                    continue;
                }
                if (!used.contains(vj)) {
                    used.add(vj);
                    if (tryNei(th + 1, vi, ui, used, to_check)) {
                        return true;
                    }
                    used.remove(vj);
                }
            }
        }
        return false;
    }

    //检测单射
    public static boolean checkNei(int vi, int ui) {
        for (int uj: Q.get(ui).nei) {
            if (!G.get(vi).cand.get(ui).keySet().contains(uj)) {
                return false;
            } else if (!G.get(vi).cand.get(ui).get(uj).isEmpty()) {
                continue;
            } else {
                return false;
            }
        }
        for (Map.Entry<Integer, List<Integer>> rep_nei: Q.get(ui).rep_nei.entrySet()) {
            Set<Integer> used = new HashSet<>();
            // 检查重复结点
            if (!tryNei(0, vi, ui, used, rep_nei.getValue())) {
                return false;
            }
        }
        return true;
    }

    public static void deleteAndCheck(int ui, int vi) {
        for (Map.Entry<Integer, Set<Integer>> nei: G.get(vi).cand.get(ui).entrySet()) {
//            System.out.print("vi:" + vi + " ui:" + ui + " nei key + val:");
//            System.out.print(nei.getKey() + " ");
//            for (int i : nei.getValue()) {
//                System.out.print(i + " ");
//            }
//            System.out.println("G.get(vj).LI.get(uj)" + G.get(1).LI.get(2));
            int uj = nei.getKey();
            for (int vj: nei.getValue()) {
//                System.out.println("判断 vi:" + vi + " ui:" + ui + " vj:" + vj + " uj:" + uj);
//                System.out.println(G.get(vj).LI.get(uj));
                if (G.get(vj).label != Q.get(uj).label) {
//                    System.out.println(111);
                    G.get(vj).cand.remove(uj);
                    G.get(vj).LI.remove(uj);
                    continue;
                }
                if (G.get(vj).LI.get(uj)) {
//                    System.out.println("del vi:" + vi + " ui:" + ui + " vj:" + vj + " uj:" + uj);
                    G.get(vj).cand.get(uj).get(ui).remove(vi);
//                    System.out.println();
                    if (G.get(vj).cand.get(uj).get(ui).isEmpty()) {
//                        System.out.println(222);
//                        System.out.println("vj:" + vj + " uj:" + uj);
                        G.get(vj).LI.put(uj, false);
                        deleteAndCheck(uj, vj);
                    } else {
//                        System.out.println(333);
                        int lb = Q.get(ui).label;
                        if (Q.get(uj).rep_nei.containsKey(lb)) {
                            List<Integer> rep_nei = Q.get(uj).rep_nei.get(lb);
                            Set<Integer> used = new HashSet<>();
//                            System.out.println(4444);
                            if (!tryNei(0, vj, uj, used, rep_nei)) {
                                G.get(vj).LI.put(uj, false);
                                deleteAndCheck(uj, vj);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void turnOff(int vi) {
        for (Map.Entry<Integer, Map<Integer, Set<Integer>>> candi: G.get(vi).cand.entrySet()) {
//            System.out.println("test" + G.get(0).LI.get(2));
            int ui = candi.getKey();
//            System.out.println("刚进入turnoff vi:" + vi + " ui:" + ui);
            if (G.get(vi).label != Q.get(ui).label) {
//                System.out.println("aa");
//                System.out.println("111vi:" + vi + " ui" + ui);
                G.get(vi).cand.remove(ui);
                G.get(vi).LI.remove(ui);
                continue;
            }
            if (!G.get(vi).LI.get(ui)) {
//                System.out.println("bb   vi:" + vi + " ui" + ui);
//                System.out.println(G.get(vi).LI.get(ui));
//                System.out.println("222vi:" + vi + " ui" + ui);
                continue;
            }
            //检查是否inject match
//            System.out.println("vi:" + vi + " ui:" + ui + "    " + checkNei(vi, ui));
            if (!checkNei(vi, ui)) {
                G.get(vi).LI.put(ui, false);
                deleteAndCheck(ui, vi);
            }
        }
    }

    public static void staticFilter() {
        for (int vi = 0; vi < G_size; vi++) {
            turnOff(vi);
        }
    }

    public static boolean notExit(int shell, Set<Integer> nei, Map<Integer, Integer> m) {
        if (nei.size() == 1) {
            return false;
        }
        int n = nei.iterator().next();
        for (int cand : G.get(m.get(n)).cand.get(n).get(shell)) {
            int count = 0;
            for (int ni : nei) {
                count += 1;
                if (count == 1) {
                    continue;
                } else {
                    if (G.get(m.get(ni)).cand.get(ni).get(shell).contains(cand)) {
                        if (count == nei.size()) {
                            return false;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return true;
    }
    // 外壳匹配获取最终匹配数量
    public static int numAdd(int th, List<Set<Integer>> cand, Set<Integer> used) {
        int result = 0;
        if (th == cand.size() - 1) {
            int del = 0;
            for (int vth : used) {
                if (cand.get(th).contains(vth)) {
                    del += 1;
                }
            }
            return cand.get(th).size() - del;
        }
        for (int vth : cand.get(th)) {
            if (!used.contains(vth)) {
                used.add(vth);
                result += numAdd(th + 1, cand, used);
                used.remove(vth);
            }
        }
        return result;
    }
    //计算两个集合的交集
    public static Set<Integer> intersection(Set<Integer> us1, Set<Integer> us2) {
        Set<Integer> result = new HashSet<>();
//        System.out.println(us1.size());
//        System.out.println("u1size");
        if (us1.size() <= us2.size()) {
            for (int item : us1) {
                if (us2.contains(item)) {
                    result.add(item);
                }
            }
        } else {
            for (int item : us2) {
                if (us1.contains(item)) {
                    result.add(item);
                }
            }
        }
        return result;
    }


    //判断外壳顶点的候选结点是否存在
    public static boolean shellCand(List<Set<Integer>> result, Map<Integer, Integer> m, List<Integer> s, List<Set<Integer>> s_n, List<Integer> used) {
        int s_size = s.size();
        for (int i = 0; i < s_size; i++) {
            result.add(new HashSet<>());
        }

//        result.addAll(new ArrayList<>(Collections.nCopies(s_size, new HashSet<>())));
        for (int i = 0; i < s_size; i++) {
            Iterator<Integer> p_ui = s_n.get(i).iterator();
            if (s_n.get(i).size() > 1) {
                int ui_1 = p_ui.next();
                int ui = p_ui.next();
                result.set(i, intersection(G.get(m.get(ui_1)).cand.get(ui_1).get(s.get(i)),
                        G.get(m.get(ui)).cand.get(ui).get(s.get(i))));
                while (p_ui.hasNext()) {
                    ui = p_ui.next();
                    result.set(i, intersection(result.get(i), G.get(m.get(ui)).cand.get(ui).get(s.get(i))));
                }
                if (result.get(i).isEmpty()) {
                    return true;
                }
            } else {
                int ui = p_ui.next();
                // *****
                result.set(i, new HashSet<>(G.get(m.get(ui)).cand.get(ui).get(s.get(i))));
            }
//            System.out.println("444");
//            for (Map.Entry<Integer, Set<Integer>> candi1 : G.get(118).cand.get(0).entrySet()) {
//                System.out.print("v1 " + "118" + " u1 " + "0" + " u2 " + candi1.getKey() + " size:" + candi1.getValue().size() + " ");
//            }
//            System.out.println();
//            System.out.println("444");
            for (int vth : used) {
                result.get(i).remove(vth);
            }
//            System.out.println("555");
//            for (Map.Entry<Integer, Set<Integer>> candi1 : G.get(118).cand.get(0).entrySet()) {
//                System.out.print("v1 " + "118" + " u1 " + "0" + " u2 " + candi1.getKey() + " size:" + candi1.getValue().size() + " ");
//            }
//            System.out.println();
//            System.out.println("555");
            if (result.get(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }



    //mark
    public static int searchCore(int th, Map<Integer, Integer> m, List<Integer> used, List<Integer> c, List<Set<Integer>> c_n, List<Integer> s, List<Set<Integer>> s_n, Map<Integer, Set<Integer>> c2check) {
        int result = 0;
        if (th == c.size()) {
            List<Set<Integer>> candidates = new ArrayList<>();
//            System.out.println("111");
//            for (Map.Entry<Integer, Set<Integer>> candi1 : G.get(118).cand.get(0).entrySet()) {
//                System.out.print("v1 " + "118" + " u1 " + "0" + " u2 " + candi1.getKey() + " size:" + candi1.getValue().size() + " ");
//            }
//            System.out.println();
//            System.out.println("111");
            if (shellCand(candidates, m, s, s_n, used)) {
                return 0;
            }
//            System.out.println("-----");
//            for (Map.Entry<Integer, Set<Integer>> candi1 : G.get(118).cand.get(0).entrySet()) {
//                System.out.print("v1 " + "118" + " u1 " + "0" + " u2 " + candi1.getKey() + " size:" + candi1.getValue().size() + " ");
//            }
//            System.out.println();
//            System.out.println("-----");
            Set<Integer> used_v = new HashSet<>();
            return numAdd(0, candidates, used_v);
        }
        Set<Integer> candidates = new HashSet<>();
        Iterator<Integer> p_ui = c_n.get(th).iterator();
        int ui = p_ui.next();
        //多个邻接结点分别处理
        if (c_n.get(th).size() > 1) {
            Set<Integer> temp = G.get(m.get(ui)).cand.get(ui).get(c.get(th));
            candidates = intersection(temp, G.get(m.get(ui)).cand.get(ui).get(c.get(th)));
            while (p_ui.hasNext()) {
                ui = p_ui.next();
                candidates = intersection(candidates, G.get(m.get(ui)).cand.get(ui).get(c.get(th)));
                if (candidates.isEmpty()) {
                    return 0;
                }
            }
            for (int vi : used) {
                candidates.remove(vi);
            }
            for (int vth : candidates) {
                m.put(c.get(th), vth);
                if (c2check.containsKey(c.get(th))) {
                    boolean to_continue = false;
                    for (int shell : c2check.get(c.get(th))) {
                        if (notExit(s.get(shell), s_n.get(shell), m)) {
                            to_continue = true;
                        }
                    }
                    if (to_continue) {
                        continue;
                    }
                }
                used.add(vth);
                result += searchCore(th + 1, m, used, c, c_n, s, s_n, c2check);
                used.remove(used.size() - 1);
            }
//            System.out.println("222");
//            for (Map.Entry<Integer, Set<Integer>> candi1 : G.get(118).cand.get(0).entrySet()) {
//                System.out.print("v1 " + "118" + " u1 " + "0" + " u2 " + candi1.getKey() + " size:" + candi1.getValue().size() + " ");
//            }
//            System.out.println();
//            System.out.println("222");
        } else {
            //单个邻接结点处理
            candidates = G.get(m.get(ui)).cand.get(ui).get(c.get(th));
            for (int vth : candidates) {
                if (!isInVec(vth, used)) {
                    m.put(c.get(th), vth);
                    if (c2check.containsKey(c.get(th))) {
                        boolean to_continue = false;
                        for (int shell : c2check.get(c.get(th))) {
                            if (notExit(s.get(shell), s_n.get(shell), m)) {
                                to_continue = true;
                            }
                        }
                        if (to_continue) {
                            continue;
                        }
                    }
                    used.add(vth);
                    result += searchCore(th + 1, m, used, c, c_n, s, s_n, c2check);
                    used.remove(used.size() - 1);
                }
            }
//            System.out.println("333");
//            for (Map.Entry<Integer, Set<Integer>> candi1 : G.get(118).cand.get(0).entrySet()) {
//                System.out.print("v1 " + "118" + " u1 " + "0" + " u2 " + candi1.getKey() + " size:" + candi1.getValue().size() + " ");
//            }
//            System.out.println();
//            System.out.println("333");
        }
//        System.out.println("result" + result);
        return result;
    }

    public static int searchMatch(int v1, int v2) {
        int update_result = 0;
        for (Map.Entry<Integer, Boolean> li: G.get(v1).LI.entrySet()) {
            if (li.getValue()) {
                int u1 = li.getKey();
                for (Map.Entry<Integer, Set<Integer>> candi: G.get(v1).cand.get(u1).entrySet()) {
                    int u2 = candi.getKey();
//                    System.out.print("u2:" + u2 + " v2:" + v2 );
//                    if (candi.getValue().contains(v2)) {
//                        cnt++;
//                    }
//                    System.out.println(" cnt" + cnt);

//                    System.out.println(cnt + "v1 " + v1 + " u2:" + u2 + " v2:" + v2 );
//                    System.out.print("v1 " + v1 + " u1 " + u1 + " u2 " + u2 + " contains:" );
//                    for (int n: candi.getValue()) {
//                        System.out.print (" " + n);
//                    }
//                    System.out.println();
//                    System.out.println("u2 1 " + G.get(118).cand.get(0).get(1).size());
//                    System.out.println("u2 2 " + G.get(118).cand.get(0).get(2).size());
//                    System.out.println("u2 3 " + G.get(118).cand.get(0).get(3).size());

//                    for (Map.Entry<Integer, Set<Integer>> candi1 : G.get(118).cand.get(0).entrySet()) {
//                        System.out.print("v1 " + "118" + " u1 " + "0" + " u2 " + candi1.getKey() + " size:" + candi1.getValue().size() + " ");
//                    }
//                    System.out.println();
                    if (u2 >= 0 && candi.getValue().contains(v2)) {
                        cnt++;
                        Map<Integer, Integer> matching = new HashMap<>();
                        matching.put(u1, v1);
                        matching.put(u2, v2);
                        List<Integer> core_v = new ArrayList<>();
                        core_v.add(v1);
                        core_v.add(v2);
//                        System.out.println("start");
//                        for (Map.Entry<Integer, Set<Integer>> candi1 : G.get(118).cand.get(0).entrySet()) {
//                            System.out.print("v1 " + "118" + " u1 " + "0" + " u2 " + candi1.getKey() + " size:" + candi1.getValue().size() + " ");
//                        }
//                        System.out.println();
//                        System.out.println("start");
//                        System.out.println(matching_order.get(u1).get(u2).core.size() + "11111111");
                        update_result += searchCore(0, matching, core_v, matching_order.get(u1).get(u2).core,
                                matching_order.get(u1).get(u2).core_nei, matching_order.get(u1).get(u2).shell,
                                matching_order.get(u1).get(u2).shell_nei, matching_order.get(u1).get(u2).c_s_nei);
                    }
                }
            }
        }
        return update_result;
    }

    public static void turnOffProcess(int v1, int v2) {
        G.get(v1).nei.remove(v2);
        G.get(v2).nei.remove(v1);

        for (Map.Entry<Integer, Map<Integer, Set<Integer>>> candi : G.get(v1).cand.entrySet()) {
            int ui = candi.getKey();
            if (G.get(v1).label != Q.get(ui).label) {
                G.get(v1).cand.remove(ui);
                G.get(v1).LI.remove(ui);
                continue;
            }
            for (Map.Entry<Integer, Set<Integer>> ui_nei : candi.getValue().entrySet()) {
                int uj = ui_nei.getKey();
                if (Q.get(uj).label == G.get(v2).label) {
                    G.get(v1).cand.get(ui).get(uj).remove(v2);
                    G.get(v2).cand.get(uj).get(ui).remove(v1);
                }
            }
        }
//        System.out.println("turnoff 前");
//        for (int vi = 0; vi < G_size; vi++) {
//            int lb = G.get(vi).label;
//            if (lb != -1) {
//                for (int ui: labels.get(lb)){
//                    System.out.println("vi:" + vi + "  " + "ui:" + ui + "  "  + G.get(vi).LI.get(ui));
//                    for (Map.Entry<Integer, Set<Integer>> map: G.get(vi).cand.get(ui).entrySet()) {
//                        System.out.print("uj:" + map.getKey() + "   vj:");
//                        for (int i: map.getValue()) {
//                            System.out.print(i + " ");
//                        }
//                        System.out.println();
//                    }
//                }
//            }
//        }
//        System.out.println("turnOff1 start");
        turnOff(v1);
//        System.out.println("turnOff1 over");
//        System.out.println("turnOff2 start");
        turnOff(v2);
//        System.out.println("turnOff2 over");
    }

    public static void addAndCheck(int ui, int vi, List<Integer> temp_v, List<Integer> temp_u) {
        for (Map.Entry<Integer, Set<Integer>> nei : G.get(vi).cand.get(ui).entrySet()) {
            int uj = nei.getKey();
            for (int vj : nei.getValue()) {
                if (G.get(vj).label != Q.get(uj).label) {
                    System.out.println("aaa");
                    G.get(vj).cand.remove(uj);
                    G.get(vj).LI.remove(uj);
                    continue;
                }
                G.get(vj).cand.get(uj).get(ui).add(vi);
                if (!G.get(vj).LI.get(uj)) {
                    if (checkNei(vj, uj)) {
                        G.get(vj).LI.put(uj, true);
                        addAndCheck(uj, vj, temp_v, temp_u);
                    } else {
                        temp_v.add(vj);
                        temp_u.add(uj);
                    }
                }
            }
        }
    }

    public static void turnOnProcess(int v1, int v2) {
        //stop_set
        List<Integer> temp_v = new ArrayList<>();
        List<Integer> temp_u = new ArrayList<>();
        G.get(v1).nei.add(v2);
        G.get(v2).nei.add(v1);
        for (Map.Entry<Integer, Map<Integer, Set<Integer>>> candi : G.get(v1).cand.entrySet()) {
            int ui = candi.getKey();
            if (G.get(v1).label != Q.get(ui).label) {
                System.out.println(111);
                G.get(v1).cand.remove(ui);
                G.get(v1).LI.remove(ui);
                continue;
            }
            for (Map.Entry<Integer, Set<Integer>> ui_nei : candi.getValue().entrySet()) {
                int uj = ui_nei.getKey();
//                if (G.get(v2).label == Q.get(uj).label) {
//                    G.get(v1).cand.get(ui).get(uj).add(v2);
//                    if (G.get(v1).LI.get(ui)) {
//                        G.get(v2).cand.get(uj).get(ui).add(v1);
//                        addAndCheck(ui, v1, temp_v, temp_u);
//                        for (int i = 0; i < temp_v.size(); i++) {
//                            int v = temp_v.get(i);
//                            int u = temp_u.get(i);
//                            if (!G.get(v).LI.get(u)) {
//                                deleteAndCheck(u, v);
//                            }
//                        }
//                        temp_v.clear();
//                        temp_u.clear();
//                    } else if (checkNei(v1, ui)) {
//                        G.get(v1).LI.put(ui, true);
//                        addAndCheck(ui, v1, temp_v, temp_u);
//                        for (int i = 0; i < temp_v.size(); i++) {
//                            int v = temp_v.get(i);
//                            int u = temp_u.get(i);
//                            if (!G.get(v).LI.get(u)) {
//                                deleteAndCheck(u, v);
//                            }
//                        }
//                        temp_v.clear();
//                        temp_u.clear();
//                    }
//                }
                if (G.get(v2).label == Q.get(uj).label) {
                    G.get(v1).cand.get(ui).get(uj).add(v2);
                    if (G.get(v1).LI.get(ui)) {
                        G.get(v2).cand.get(uj).get(ui).add(v1);
                        addAndCheck(ui, v1, temp_v, temp_u);
                        for (int i = 0; i < temp_v.size(); i++) {
                            int v = temp_v.get(i);
                            int u = temp_u.get(i);
                            if (!G.get(v).LI.get(u)) {
                                deleteAndCheck(u, v);
                            }
                        }
                        temp_v.clear();
                        temp_u.clear();
                    } else if (checkNei(v1, ui)) {
                        G.get(v1).LI.put(ui, true);
                        addAndCheck(ui, v1, temp_v, temp_u);
                        for (int i = 0; i < temp_v.size(); i++) {
                            int v = temp_v.get(i);
                            int u = temp_u.get(i);
                            if (!G.get(v).LI.get(u)) {
                                deleteAndCheck(u, v);
                            }
                        }
                        temp_v.clear();
                        temp_u.clear();
                    }
                }
            }
        }
    }

    public static void updateAndMatching() {
        long add_matches = 0;
        long del_matches = 0;
        double update_time = 0.0;
        double search_time = 0.0;
        long time = 0;

        for (int t = 0; t < update.size(); t += 2) {
            int v1 = update.get(t);
            int v2 = update.get(t + 1);

            //负数输入表示删除 先搜索匹配，再OFF传播
            //正数输入表示添加 先ON传播，再搜索匹配
            if (v1 < 0) {
//                System.out.println("e " + v1 + " " + v2);
//                System.out.println(-v1 - 1);
//                System.out.println(G.size());
                if (G.get(-v1 - 1).label == -1 || G.get(-v2 - 1).label == -1) {
                    continue;
                }
                if (!G.get(-v1 - 1).nei.contains(-v2 - 1)) {
                    continue;
                }
                time = System.currentTimeMillis();
                del_matches += searchMatch(-v1 - 1, -v2 - 1);
//                System.out.println(del_matches);
                search_time += System.currentTimeMillis() - time;
                time = System.currentTimeMillis();
                turnOffProcess(-v1 - 1, -v2 - 1);
//                System.out.println("turnOffProcess后");
//                for (int vi = 0; vi < G_size; vi++) {
//                    int lb = G.get(vi).label;
//                    if (lb != -1) {
//                        for (int ui: labels.get(lb)){
//                            System.out.println("vi:" + vi + "  " + "ui:" + ui + "  "  + G.get(vi).LI.get(ui));
//                            for (Map.Entry<Integer, Set<Integer>> map: G.get(vi).cand.get(ui).entrySet()) {
//                                System.out.print("uj:" + map.getKey() + "   vj:");
//                                for (int i: map.getValue()) {
//                                    System.out.print(i + " ");
//                                }
//                                System.out.println();
//                            }
//                        }
//                    }
//                }
//                System.out.println();
                update_time += System.currentTimeMillis() - time;
            } else {
//                System.out.println("e " + v1 + " " + v2);
                if (G.get(v1).label == -1 || G.get(v2).label == -1) {
//                    System.out.println(11);
                    continue;
                }

                if (G.get(v1).nei.contains(v2)) {
                    continue;
                }

                time = System.currentTimeMillis();
                turnOnProcess(v1, v2);
//                System.out.println("turnOnProcess后");
//                for (int vi = 0; vi < G_size; vi++) {
//                    int lb = G.get(vi).label;
//                    if (lb != -1) {
//                        for (int ui: labels.get(lb)){
//                            System.out.println("vi:" + vi + "  " + "ui:" + ui + "  "  + G.get(vi).LI.get(ui));
//                            for (Map.Entry<Integer, Set<Integer>> map: G.get(vi).cand.get(ui).entrySet()) {
//                                System.out.print("uj:" + map.getKey() + "   vj:");
//                                for (int i: map.getValue()) {
//                                    System.out.print(i + " ");
//                                }
//                                System.out.println();
//                            }
//                        }
//                    }
//                }
//                System.out.println();
                update_time += System.currentTimeMillis() - time;
                time = System.currentTimeMillis();
                add_matches += searchMatch(v1, v2);
                search_time += System.currentTimeMillis() - time;
            }
        }

        System.out.println("Added matches: " + add_matches);
        System.out.println("Deleted matches: " + del_matches);
        System.out.println("Updated matches: " + (add_matches + del_matches));
        System.out.println("Update time: " + update_time + "ms");
        System.out.println("Search time: " + search_time + "ms");
    }

    public static void main(String[] args) {
        String g_path = "./email10/initial";
        String q_path = "./email10/Q/10/q3";
        String s_path = "./email10/s";
//        String g_path = "./email10/test_g1";
//        String q_path = "./email10/test_q1";
//        String s_path = "./email10/test_s";
        long time = System.currentTimeMillis();
        System.out.println("========== Start inputting. ==========");
        inputQ(q_path);
        generateMO();
        inputG(g_path);
        inputUpdate(s_path);
        System.out.println("Inputting cost (ms): " + (System.currentTimeMillis() - time));
        System.out.println("The number of vertices in Q: " + Q_size);
        System.out.println("The number of vertices in G: " + G_size);
        System.out.println("========== End inputting. ==========");

        time = System.currentTimeMillis();
        System.out.println("========== Start constructing. ==========");
        constructCand();
        System.out.println("Constructing cost (ms): " + (System.currentTimeMillis() - time));
        System.out.println("========== End constructing. ==========");

        time = System.currentTimeMillis();
        System.out.println("========== Start static filtering. ==========");
        staticFilter();
        System.out.println("Static filtering cost (ms): " + (System.currentTimeMillis() - time));
        System.out.println("========== End static filtering. ==========");

        time = System.currentTimeMillis();
        System.out.println("========== Start updating. ==========");
        updateAndMatching();
        System.out.println("Updating totally cost (ms): " + (System.currentTimeMillis() - time));
        System.out.println("========== End updating. ==========");
//        for (int vi = 0; vi < G_size; vi++) {
//            int lb = G.get(vi).label;
//            if (lb != -1) {
//                for (int ui: labels.get(lb)){
//                    System.out.println("vi:" + vi + "  " + "ui:" + ui + "  "  + G.get(vi).LI.get(ui));
//                    for (Map.Entry<Integer, Set<Integer>> map: G.get(vi).cand.get(ui).entrySet()) {
//                        System.out.print("uj:" + map.getKey() + "   vj:");
//                        for (int i: map.getValue()) {
//                            System.out.print(i + " ");
//                        }
//                        System.out.println();
//                    }
//                }
//            }
//        }

//        for (int vi = 0; vi < G_size; vi++) {
//            int lb = G.get(vi).label;
//            if (lb != -1) {
//                for (int ui: labels.get(lb)){
//                    System.out.println("vi:" + vi + "  " + "ui:" + ui + "  "  + G.get(vi).LI.get(ui));
//                    for (Map.Entry<Integer, Set<Integer>> map: G.get(vi).cand.get(ui).entrySet()) {
//                        System.out.print("uj:" + map.getKey() + "   vj:");
//                        for (int i: map.getValue()) {
//                            System.out.print(i + " ");
//                        }
//                        System.out.println();
//                    }
//                }
//            }
//        }
        System.out.println("========== DONE!! ==========");
        System.out.println(cnt);
    }




}
