package nl.esciencecenter.aether.impl.stacking.lrmc.util;


import java.util.Arrays;
import java.util.Comparator;

import nl.esciencecenter.aether.AetherIdentifier;
import nl.esciencecenter.aether.Location;

public class AetherSorter implements Comparator<AetherIdentifier> {

    // General sorter to use when no cluster order is preferred.
    private static final AetherSorter sorter = new AetherSorter("unknown", null);

    private final String preferredCluster;
    private final String preferredName;

    private AetherSorter(String preferredCluster, String preferredName) {
        this.preferredCluster = preferredCluster;
        this.preferredName = preferredName;
    }

    public static void sort(AetherIdentifier[] ids) {
        sort(ids, 0, ids.length);
    }

    public static void sort(AetherIdentifier local, AetherIdentifier[] ids) {
        sort(local, ids, 0, ids.length);
    }

    public static void sort(AetherIdentifier[] ids, int from, int to) {
        Arrays.sort(ids, from, to, sorter);
    }

    public static void sort(AetherIdentifier local, AetherIdentifier[] ids,
            int from, int to) {
        /*
         * IbisSorter tmp = sorter;
         * 
         * if (!local.equals(sorter.preferredName) ||
         * !local.getLocation().cluster().equals(sorter.preferredCluster)) { tmp =
         * new IbisSorter(local.getLocation().cluster(), local); }
         * 
         * Arrays.sort(ids, from, to, tmp);
         */
        AetherIdentifier[] tmp = new AetherIdentifier[(to - from) + 1];
        tmp[0] = local;

        System.arraycopy(ids, from, tmp, 1, to - from);

        Arrays.sort(tmp, sorter);

        int index = 0;

        for (int i = 0; i < tmp.length; i++) {
            if (tmp[i].equals(local)) {
                index = i;
                break;
            }
        }

        System.arraycopy(tmp, index + 1, ids, from, tmp.length - (index + 1));
        System.arraycopy(tmp, 0, ids, from + tmp.length - index - 1, index);
    }

    // Returns the index of the first character that is different in the two
    // Strings. Thus, the higher the number returned, the longer the prefix that
    // the two Strings share.
    private static int firstDifference(String s1, String s2) {

        // first, make sure that we s1 is the shortest string.
        if (s1.length() > s2.length()) {
            String tmp = s1;
            s1 = s2;
            s2 = tmp;
        }

        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }

        return s1.length();
    }

    public int compare(AetherIdentifier id1, AetherIdentifier id2) {

        Location cluster1 = id1.location().getParent();
        Location cluster2 = id2.location().getParent();

        if (cluster1.equals(cluster2)) {
            // The clusters are identical, so the order depends completely
            // on the names.
            // 
            // For SMP awareness, we assume that the identifiers of two ibises
            // on an SMP machine are 'closer' that the identifiers of two ibises
            // on different machines. This way, the identifiers will
            // automatically be sorted 'pair-wise' (or quad/oct/etc, depending
            // on the number of ibises that share the SMP machines).
            // 
            // One aditional problem is that we want the ibises that share the
            // machine with the sender to be first (which isn't that simple!).

            if (preferredName == null) {
                return id1.location().toString().compareTo(
                        id2.location().toString());
            } else {
                // Figure out if one of the two strings has a longer prefix
                // in common with 'preferredName'. Note that this will result
                // in the lenght of the string only if the IbisIdentifier
                // actually contains the 'preferredName'. Therefore, this
                // IbisIdentifier will end up at the first position of the
                // array, which is exactly what we want.
                int d1 = firstDifference(preferredName, id1.location()
                        .toString());
                int d2 = firstDifference(preferredName, id2.location()
                        .toString());

                // If both have the same distance, we sort them alphabetically.
                // Otherwise, we prefer the one that is closest to
                // 'preferredName', since these may actually be located on the
                // same machine.
                if (d1 == d2) {
                    return id1.location().toString().compareTo(
                            id2.location().toString());
                } else if (d1 <= d2) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }

        // The clusters are different. If one of the two is equal to the
        // preferredCluster, we want that one to win. Otherwise, we just return
        // the 'natural order'.
        if (cluster1.equals(preferredCluster)) {
            return -1;
        }

        if (cluster2.equals(preferredCluster)) {
            return 1;
        }

        return cluster1.compareTo(cluster2);
    }
}
