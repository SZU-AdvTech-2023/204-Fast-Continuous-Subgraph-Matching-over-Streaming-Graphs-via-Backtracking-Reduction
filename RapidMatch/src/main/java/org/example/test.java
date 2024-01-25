package org.example;

import java.util.*;

public class test {

    public static void main(String[] args) {
        Map<Integer, List<Integer>> map = new HashMap();
        map.put(1, new ArrayList<>());
        map.get(1).add(1);
        map.get(1).add(1);
        map.get(1).add(1);
        List<Integer> list = map.get(1);
        System.out.println(list.size());
        Set<Integer> set = new HashSet<>();
        set.add(1);
        set.add(2);set.add(3);set.add(4);
        Iterator<Integer> it = set.iterator();
        int ui = it.next();
        int u22 = it.next();

        System.out.println(ui);
        System.out.println(u22);
        Set<Integer> set1 = new HashSet<>();
        System.out.println(set1 == null);
        System.out.println(ss(set, set1));
    }

    public static int ss(Set<Integer> s1, Set<Integer> s2) {
        return 1;
    }
}



