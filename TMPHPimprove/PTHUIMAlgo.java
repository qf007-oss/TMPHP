package THUIM;

import java.io.*;
import java.util.*;


/**
 * This is an implementation of the "THUIM Algorithm" in paper:
 * use target pattern twu order
 * Jinbao Miao et al. THUIM: Target High Utility Itemset Querying. 2021.
 *
 * @author Jinbao Miao & Wensheng Gan, JNU, China
 * @see UtilityList
 * @see Element
 */

public class PTHUIMAlgo {
    /**
     * the time at which the algorithm started
     */
    public long startTimestamp = 0;

    /**
     * the time at which the algorithm ended
     */
    public long endTimestamp = 0;

    /**
     * the number of high-utility itemsets generated
     */
    public int huiCount = 0;

    /**
     * Map to remember the TWU of each item
     */
    Map<Integer, Integer> mapItemToTWU;


    /**
     * Map to remember the item of tarParttern and their new order in TWU asc
     */
    Map<Integer, Integer> mapItemToOrder;


    /**
     * Map to remember the support smallest periodicity and largest periodicity of each item
     */
    Map<Integer, ItemInfo> mapItemToItemInfo;

    /**
     * the max length of tarPattern
     */
    int maxTarLength = 0;

    /**
     * the length of tarPattern
     */
    int tarPatternCount = 0;

    /**
     * writer to write the output file
     */
    BufferedWriter writer = null;

    /**
     * the number of utility-list that was constructed
     */
    private int joinCount;

    int minUtil;

    /**
     * buffer for storing the current itemset that is mined when performing mining
     * the idea is to always reuse the same buffer to reduce memory usage.
     */
    final int BUFFERS_SIZE = 200;
    private int[] itemsetBuffer = null;


    /**
     * the database size (number of transactions
     */
    int databaseSize = 0;

    /**
     * the periodicity threshold
     **/
    int minPeriodicity;
    int maxPeriodicity;
    int minAveragePeriodicity;
    int maxAveragePeriodicity;

    /**
     * the gamma parameter
     **/
    double supportPruningThreshold = 0;

    public boolean DEBUG = false;

    /**
     * this class represent an item and its utility in a transaction
     */
    class Pair {
        int item = 0;
        int utility = 0;
    }

    /**
     * this class represent a single item and its support and periodicity
     */
    class ItemInfo {
        int support = 0;
        int largestPeriodicity = 0;
        int smallestPeriodicity = Integer.MAX_VALUE;
        int lastSeenTransaction = 0;
    }

    /**
     * Default constructor
     */
    public PTHUIMAlgo() {
    }

    /**
     * Run the algorithm
     *
     * @param input      the input file path
     * @param output     the output file path
     * @param minUtility the minimum utility threshold
     * @throws IOException exception if error while writing the file
     */
    public void THUIM(String input, String output, int minUtility, Integer[] tarPattern,
                      int minPeriodicity, int maxPeriodicity, int minAveragePeriodicity, int maxAveragePeriodicity) throws IOException {
        // reset maximum, here the maxMemory = 0
        MemoryLogger.getInstance().reset();

        this.minUtil = minUtility;
        this.maxPeriodicity = maxPeriodicity;
        this.minPeriodicity = minPeriodicity;
        this.minAveragePeriodicity = minAveragePeriodicity;
        this.maxAveragePeriodicity = maxAveragePeriodicity;

        // initialize the buffer for storing the current itemset
        itemsetBuffer = new int[BUFFERS_SIZE];

        startTimestamp = System.currentTimeMillis();

        writer = new BufferedWriter(new FileWriter(output));

        //  We create a  map to store the TWU of each item
        mapItemToTWU = new HashMap<Integer, Integer>();

        // We create a map to store the support and period information of each item
        mapItemToItemInfo = new HashMap<Integer, ItemInfo>();

        // We create a map to store the order of the tarHUISubsume
        mapItemToOrder = new HashMap<Integer, Integer>();

        // the length of the target Pattern
        tarPatternCount = tarPattern.length;

        // ========================= improve ==============================
        // We create a queue to store the transaction that all target items occurs.
        Queue<Integer> targetTid = new ArrayDeque<>();
        // ========================= improve end ==============================

        maxTarLength = 0;

        // We scan the database a first time to calculate the TWU of each item.
        BufferedReader myInput = null;
        databaseSize = 0;
        String thisLine;
        long sumOfTransactionLength = 0;  // for debugging

        try {
            // prepare the object for reading the file
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
            // for each line (transaction) until the end of file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a
                // kind of metadata
                if (thisLine.isEmpty() == true ||
                        thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }

                // increase the number of transactions
                databaseSize++;

                // split the transaction according to the : separator
                String split[] = thisLine.split(":");
                // the first part is the list of items
                String items[] = split[0].split(" ");
                // the second part is the transaction utility
                int transactionUtility = Integer.parseInt(split[1]);

                sumOfTransactionLength += items.length;


                // ========================= improve ==============================
                Set<Integer> targetSet = new HashSet<>(Arrays.asList(tarPattern));
                Set<Integer> dataSet = new HashSet<>();
                for (String num : items) {
                    Integer item = Integer.parseInt(num);
                    dataSet.add(item);
                    targetSet.remove(item);
                    if (targetSet.isEmpty()) {
                        break;
                    }
                }
                if (targetSet.isEmpty()) {
                    targetTid.add(databaseSize);
                } else {
                    continue;
                }
                // ========================= improve end ==============================


                // for each item, we add the transaction utility to its TWU
                for (int i = 0; i < items.length; i++) {
                    // convert item to integer
                    Integer item = Integer.parseInt(items[i]);
                    // get the current TWU of that item
                    Integer twu = mapItemToTWU.get(item);
                    // add the utility of the item in the current transaction to its twu
                    twu = (twu == null) ?
                            transactionUtility : twu + transactionUtility;
                    mapItemToTWU.put(item, twu);

                    // we also add 1 to the support of the item
                    ItemInfo itemInfo = mapItemToItemInfo.get(item);
                    if (itemInfo == null) {
                        itemInfo = new ItemInfo();
                        mapItemToItemInfo.put(item, itemInfo);
                    }
                    // increase support
                    itemInfo.support++;

                    // **** PHM ***********
                    // calculate periodicity
                    int periodicity = databaseSize - itemInfo.lastSeenTransaction;
                    // update periodicity of this item
                    if (itemInfo.largestPeriodicity < periodicity) {
                        itemInfo.largestPeriodicity = periodicity;
                    }
                    itemInfo.lastSeenTransaction = databaseSize;

                    // IF IT IS not the first time that we see the item, we update
                    // its minimum periodicity
                    if (itemInfo.support != 1 && periodicity < itemInfo.smallestPeriodicity) {
                        itemInfo.smallestPeriodicity = periodicity;
                    }


                }
            }
        } catch (Exception e) {
            // catches exception if error while reading the input file
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
            }
        }

        supportPruningThreshold = (((double) databaseSize) / ((double) maxAveragePeriodicity)) - 1d;


        // **** PHM ***********
        // calculate the last period.
        for (Map.Entry<Integer, ItemInfo> entry : mapItemToItemInfo.entrySet()) {
            ItemInfo itemInfo = entry.getValue();

            // calculate the last period
            int periodicity = databaseSize - itemInfo.lastSeenTransaction;

            // update periodicity of this item
            if (itemInfo.largestPeriodicity < periodicity) {
                itemInfo.largestPeriodicity = periodicity;
            }

            // Important: we do not update the minimum periodicity of the item using its last period.
            if (DEBUG) {
                System.out.println(" item : " + entry.getKey()
                        + "\tavgPer: " + (databaseSize / (double) (itemInfo.support + 1))
                        + "\tminPer: " + itemInfo.smallestPeriodicity
                        + "\tmaxPer: " + itemInfo.largestPeriodicity
                        + "\tTWU: " + mapItemToTWU.get(entry.getKey())
                        + "\tsup.: " + itemInfo.support
                );
            }
        }
        if (DEBUG) {
            System.out.println("Number of transactions : " + databaseSize);
            System.out.println("Average transaction length : " + sumOfTransactionLength / (double) databaseSize);
            System.out.println("Number of items : " + mapItemToItemInfo.size());
            System.out.println("Average pruning threshold  (|D| / maxAvg $) - 1): " + supportPruningThreshold);
        }

        // CREATE A LIST TO STORE THE UTILITY LIST OF ITEMS WITH TWU  >= MIN_UTILITY.
        List<UtilityList> listOfUtilityLists = new ArrayList<UtilityList>();
        // CREATE A MAP TO STORE THE UTILITY LIST FOR EACH ITEM.
        // Key : item    Value :  utility list associated to that item
        Map<Integer, UtilityList> mapItemToUtilityList = new HashMap<Integer, UtilityList>();


        // For each item
        for (Integer item : mapItemToTWU.keySet()) {
            ItemInfo itemInfo = mapItemToItemInfo.get(item);
            // if the item is promising  (TWU >= minutility)
            // System.out.println(item + " TWU: " + mapItemToTWU.get(item));
            if (itemInfo.support >= supportPruningThreshold &&
                    itemInfo.largestPeriodicity <= maxPeriodicity &&
                    mapItemToTWU.get(item) >= minUtility) {
                // create an empty Utility List that we will fill later.
                UtilityList uList = new UtilityList(item);
                mapItemToUtilityList.put(item, uList);
                // add the item to the list of high TWU items
                listOfUtilityLists.add(uList);
                ///*************** PHM ****************
                // set the periodicity
                uList.largestPeriodicity = itemInfo.largestPeriodicity;
                uList.smallestPeriodicity = itemInfo.smallestPeriodicity;
                ///*************** END PHM ****************

            }
        }

        // there have item that does not satisfy the TWU or maxPer or maxAvg Pruning in target Pattern, we will be terminated.
        for (Integer item : tarPattern) {
            ItemInfo itemInfo = mapItemToItemInfo.get(item);
            if (itemInfo.support < supportPruningThreshold ||
                    itemInfo.largestPeriodicity > maxPeriodicity ||
                    mapItemToTWU.get(item) < minUtility) return;
        }

        // SORT THE LIST OF HIGH TWU ITEMS IN ASCENDING ORDER
        Collections.sort(listOfUtilityLists, new Comparator<UtilityList>() {
            public int compare(UtilityList o1, UtilityList o2) {
                // compare the TWU of the items, TWU ascend order.
                return compareItems(o1.item, o2.item);
            }
        });

        // test item order
        /*for (UtilityList list : listOfUtilityLists) {
            System.out.print(list.item + " ");
        }

        System.out.println();
        for (Map.Entry<Integer, Integer> entry : mapItemToTWU.entrySet()) {
            Integer item = entry.getKey();
            Integer ItemTWU = entry.getValue();
            System.out.println(item +": "+ItemTWU);
        }*/

        Arrays.sort(tarPattern, new Comparator<Integer>() {
            public int compare(Integer o1, Integer o2) {
                return compareItems(o1, o2);
            }
        });

        if (DEBUG) {
            System.out.println(Arrays.toString(tarPattern));
        }
        //System.out.println(tarPattern.toString());
        // set the flag ****
        int pos = 0;

        for (UtilityList X : listOfUtilityLists) {

            //System.out.println(X.item + " TWU: " + mapItemToTWU.get(X.item));

            mapItemToOrder.put(X.item, ++pos);
        }

        /*// test itemOrder
        for (Map.Entry<Integer, Integer> entry : mapItemToOrder.entrySet()) {
            Integer item = entry.getKey();
            Integer itemOrder = entry.getValue();
            System.out.println(item + " = " + itemOrder);
        }*/

        // SECOND DATABASE PASS TO CONSTRUCT THE UTILITY LISTS
        // OF 1-ITEMSETS  HAVING TWU >= minutil (promising items)
        try {
            // prepare object for reading the file
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
            // variable to count the number of transaction
            int tid = 0;

            // for each line (transaction) until the end of file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a
                // kind of metadata
                if (thisLine.isEmpty() == true ||
                        thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }

                // ========================= improve ==============================
                // this transaction does not all target items, we ignore it.
                // there the tid start from 0.
                Integer peek = targetTid.peek();
                // if the targetTid are 1 2 4 5 7 and the tid are 8 9 10.
                if(peek==null){
                    break;
                }
                /*System.out.println("======test======");
                System.out.println(peek);*/
                if((tid + 1) != peek){
                    tid++;
                    continue;
                }else{
                    targetTid.poll();
                }
                // ========================= improve end ==============================

                // split the line according to the separator
                String split[] = thisLine.split(":");
                // get the list of items
                String items[] = split[0].split(" ");
                // get the list of utility values corresponding to each item
                // for that transaction
                String utilityValues[] = split[2].split(" ");

                // Copy the transaction into lists but
                // without items with TWU < minutility

                int remainingUtility = 0;

                // Create a list to store items
                List<Pair> revisedTransaction = new ArrayList<Pair>();
                // for each item
                for (int i = 0; i < items.length; i++) {
                    /// convert values to integers
                    Pair pair = new Pair();
                    pair.item = Integer.parseInt(items[i]);
                    pair.utility = Integer.parseInt(utilityValues[i]);
                    ItemInfo itemInfo = mapItemToItemInfo.get(pair.item);
                    // if the item has enough utility
                    if (itemInfo.support >= supportPruningThreshold &&
                            itemInfo.largestPeriodicity <= maxPeriodicity &&
                            mapItemToTWU.get(pair.item) >= minUtility) {
                        // add it
                        revisedTransaction.add(pair);
                        remainingUtility += pair.utility;
                    }
                }

                Collections.sort(revisedTransaction, new Comparator<Pair>() {
                    public int compare(Pair o1, Pair o2) {
                        return compareItems(o1.item, o2.item);
                    }
                });


                // for each item left in the transaction
                for (Pair pair : revisedTransaction) {
                    // subtract the utility of this item from the remaining utility
                    remainingUtility = remainingUtility - pair.utility;

                    // get the utility list of this item
                    UtilityList utilityListOfItem = mapItemToUtilityList.get(pair.item);

                    // Add a new Element to the utility list of this item corresponding to this transaction
                    Element element = new Element(tid, pair.utility, remainingUtility);

                    utilityListOfItem.addElement(element);
                }
                tid++; // increase tid number for next transaction

            }
        } catch (Exception e) {
            // to catch error while reading the input file
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
            }
        }

        /*// **** Release the memory for the maps ****
        mapItemToItemInfo = null;
        mapItemToTWU = null;
        mapItemToUtilityList = null;*/

        // check the memory usage
        MemoryLogger.getInstance().checkMemory();

        // Mine the database recursively
        THUIM_miner(itemsetBuffer, 0, null, listOfUtilityLists, minUtility, 0, tarPattern);

        // check the memory usage again and close the file.
        MemoryLogger.getInstance().checkMemory();
        // close output file
        writer.close();
        // record end time
        endTimestamp = System.currentTimeMillis();
    }

    private int compareItems(int item1, int item2) {
        // TWU ascend order
        int compare = mapItemToTWU.get(item1) - mapItemToTWU.get(item2);
        // if the same, use the lexical order otherwise use the TWU
        return (compare == 0) ? item1 - item2 : compare;
    }

    /**
     * This is the recursive method to find all high utility itemsets. It writes
     * the itemsets to the output file.
     *
     * @param prefix       This is the current prefix. Initially, it is empty.
     * @param pUL          This is the Utility List of the prefix. Initially, it is empty.
     * @param ULs          The utility lists corresponding to each extension of the prefix.
     * @param minUtility   The minUtility threshold.
     * @param prefixLength The current prefix length
     * @throws IOException
     */
    private void THUIM_miner(int[] prefix, int prefixLength, UtilityList pUL, List<UtilityList> ULs, int minUtility, int index, Integer[] tarPattern)
            throws IOException {
        // prefix : null, prefixLength : 0, pUL : null, ULs : a b c d e, minUtility, index : 0, tarPattern.

        // For each extension X of prefix P
        if (ULs == null) return;

        /*System.out.println("===========================");
        System.out.println("ULs.size(): "+ULs.size());
        for (UtilityList ul : ULs) {
            System.out.print(ul.item + " ");
        }
        System.out.println();*/

        for (int i = 0; i < ULs.size(); i++) {
            // make sure update subCount every time
            UtilityList X = ULs.get(i);
            int currentindex = index, oriorder = 0, patorder = 0;

            if (currentindex < tarPatternCount) {
                oriorder = mapItemToOrder.get(X.item);

                patorder = mapItemToOrder.get(tarPattern[currentindex]);

                if (oriorder == patorder) currentindex++;
            }

            if (currentindex < tarPatternCount && patorder < oriorder) break;

            double averagePeriodicity = (double) databaseSize / ((double) X.getSupport() + 1);

            // If pX is a high utility itemset.
            // we save the itemset:  pX
            if (X.sumIutils >= minUtility
                    && currentindex >= tarPatternCount
                    && averagePeriodicity <= maxAveragePeriodicity
                    && averagePeriodicity >= minAveragePeriodicity
                    && X.smallestPeriodicity >= minPeriodicity
                    && X.largestPeriodicity <= maxPeriodicity) {
                // save to file
                writeOut(prefix, prefixLength, X, currentindex, averagePeriodicity);
            }

            // If the sum of the remaining utilities for pX
            // is higher than minUtility, we explore extensions of pX.
            // (this is the pruning condition)
            // ============================== //

            // 2 1 3 4 5 7
            // X
            //   1 3
            //   0

            if (X.sumIutils + X.sumRutils >= minUtility) {
                // This list will contain the utility lists of pX extensions.
                List<UtilityList> exULs = new ArrayList<UtilityList>();
                // For each extension of p appearing
                // after X according to the ascending order
                for (int j = i + 1; j < ULs.size(); j++) {
                    // the all can combine with X,can't only judge one item
                    UtilityList Y = ULs.get(j);

                    UtilityList construct = construct(pUL, X, Y, minUtility);

                    if(construct!=null){
                        exULs.add(construct);
                    }

                    // construct table of xy include it tid and utility
                    joinCount++;
                }
                // We create new prefix pX
                //System.out.println("exULs.size : "+ exULs.size());
                itemsetBuffer[prefixLength] = X.item; // itemsetBuffer can make sure the subsume maxlength

                // We make a recursive call to discover all itemsets with the prefix pXY
                //System.out.println("end");
                THUIM_miner(itemsetBuffer, prefixLength + 1, X, exULs, minUtility, currentindex, tarPattern);
            }
        }
    }

    /**
     * This method constructs the utility list of pXY
     *
     * @param P  :  the utility list of prefix P.
     * @param px : the utility list of pX
     * @param py : the utility list of pY
     * @return the utility list of pXY
     */
    private UtilityList construct(UtilityList P, UtilityList px, UtilityList py, int minUtility) {
        //System.out.println("*********" + px.item +" "+ py.item);
        // create an empy utility list for pXY
        //System.out.println("==================");
        UtilityList pxyUL = new UtilityList(py.item); // only have Y
        int lastTid = -1;  // IMPORTANT BECAUSE TIDS STARTS AT ZERO...!!

        //== new optimization - LA-prune  == /
        // Initialize the sum of total utility
        long totalUtility = px.sumIutils + px.sumRutils;
        // A similar strategy to LA-prune will be applied for the support
        // Initialize the sum of support
        long totalSupport = px.getSupport();

        // for each element in the utility list of pX
        for (Element ex : px.elements) {
            // do a binary search to find element ey in py with tid = ex.tid
            Element ey = findElementWithTID(py, ex.tid);
            if (ey == null) {
                totalUtility -= (ex.iutils + ex.rutils);
                if (totalUtility < minUtility) {
                    return null;
                }
                // decrease the support by one transaction
                totalSupport -= 1;
                if (totalSupport < supportPruningThreshold) {
                    return null;
                }
                continue;
            }
            // if the prefix p is null
            if (P == null) {
                // check the periodicity
                int periodicity = ex.tid - lastTid;
                if (periodicity > maxPeriodicity) {
                    return null;
                }
                if (periodicity >= pxyUL.largestPeriodicity) {
                    pxyUL.largestPeriodicity = periodicity;
                }
                lastTid = ex.tid;

                // IMPORTANT DO NOT COUNT THE FIRST PERIOD FOR MINIMUM UTILITY
                if (pxyUL.elements.size() > 0 && periodicity < pxyUL.smallestPeriodicity) {
                    pxyUL.smallestPeriodicity = periodicity;
                }

                // Create the new element
                Element eXY = new Element(ex.tid, ex.iutils + ey.iutils, ey.rutils);
                // add the new element to the utility list of pXY
                pxyUL.addElement(eXY);

            } else {
                // find the element in the utility list of p wih the same tid
                Element e = findElementWithTID(P, ex.tid);
                if (e != null) {
                    // ********** PHM *************
                    // check the periodicity
                    int periodicity = ex.tid - lastTid;
                    if (periodicity > maxPeriodicity) {
                        return null;
                    }
                    if (periodicity >= pxyUL.largestPeriodicity) {
                        pxyUL.largestPeriodicity = periodicity;
                    }
                    lastTid = ex.tid;

                    // IMPORTANT DO NOT COUNT THE FIRST PERIOD FOR MINIMUM UTILITY
                    if (pxyUL.elements.size() > 0 && periodicity < pxyUL.smallestPeriodicity) {
                        pxyUL.smallestPeriodicity = periodicity;
                    }
                    // ********** END PHM *************

                    // Create new element
                    Element eXY = new Element(ex.tid, ex.iutils + ey.iutils - e.iutils,
                            ey.rutils);
                    // add the new element to the utility list of pXY
                    pxyUL.addElement(eXY);
                }
            }
        }
        // ********** PHM *************
        // check the periodicity
        int periodicity = (databaseSize - 1) - lastTid;  // Need -1 because tids starts at zero
//		if(P==null && px.item == 4 && py.item == 2){
//			System.out.println("period : " + periodicity);
//		}

        if (periodicity > maxPeriodicity) {
            return null;
        }
        if (periodicity >= pxyUL.largestPeriodicity) {
            pxyUL.largestPeriodicity = periodicity;
        }

        if (pxyUL.getSupport() < supportPruningThreshold) {
            return null;
        }

        // WE DO NOT UPDATE THE MINIMUM PERIOD
//		if(pxyUL.smallestPeriodicity > maxAveragePeriodicity){
//			return null;
//		}

        // return the utility list of pXY.
        return pxyUL;
    }

    /**
     * Do a binary search to find the element with a given tid in a utility list
     *
     * @param ulist the utility list
     * @param tid   the tid
     * @return the element or null if none has the tid.
     */
    private Element findElementWithTID(UtilityList ulist, int tid) {
        List<Element> list = ulist.elements;

        // perform a binary search to check if  the subset appears in  level k-1.
        int first = 0;
        int last = list.size() - 1;

        // the binary search
        while (first <= last) {
            int middle = (first + last) >>> 1; // divide by 2

            if (list.get(middle).tid < tid) {
                first = middle + 1;
                //  the itemset compared is larger than the subset according to the lexical order
            } else if (list.get(middle).tid > tid) {
                last = middle - 1;
                //  the itemset compared is smaller than the subset  is smaller according to the lexical order
            } else {
                return list.get(middle);
            }
        }
        return null;
    }

    /**
     * Method to write a high utility itemset to the output file.
     *
     * @param prefix:             the prefix to be write to the output file
     * @param utilityList:        an utilityList respect the last item to be appended to the prefix
     * @param averagePeriodicity: the averagePeriod of the itemSet
     * @param prefixLength:       the prefix length
     */
    private void writeOut(int[] prefix, int prefixLength, UtilityList utilityList, int index, double averagePeriodicity) throws IOException {
        huiCount++; // increase the number of high utility itemsets found

        //Create a string buffer
        StringBuilder buffer = new StringBuilder();
        // append the prefix
        for (int i = 0; i < prefixLength; i++) {
            buffer.append(prefix[i]);
            buffer.append(' ');
        }
        // append the last item
        buffer.append(utilityList.item);
        // append the utility value
        buffer.append(" #UTIL: ");
        buffer.append(utilityList.sumIutils);
        // write to file

        buffer.append(" #MINPER: ");
        buffer.append(utilityList.smallestPeriodicity);

        // append the largest periodicity
        buffer.append(" #MAXPER: ");
        buffer.append(utilityList.largestPeriodicity);

        // append the average periodicity
        buffer.append(" #AVGPER: ");
        buffer.append(averagePeriodicity);

        buffer.append(" #Target: ");
        buffer.append(index);

        writer.write(buffer.toString());
        writer.newLine();
    }

    /**
     * Print statistics about the latest execution to System.out.
     * pruning for minging Target Pattern
     */
    public void printStats(String input, int minutil, Integer[] tararray) {
        System.out.println("========== TMPHP ALGORITHM v1.0 - STATS  ==========");
        System.out.println(" Input file: " + input.toString());
        System.out.println(" minimum utility: " + minutil);
        System.out.println(" Target pattern: " + Arrays.toString(tararray));
        System.out.println(" Total time: " + (endTimestamp - startTimestamp) / 1000.0 + " s");
        System.out.println(" Maximal memory: " + MemoryLogger.getInstance().getMaxMemory() + " MB");
        System.out.println(" Target PHUIs count: " + huiCount);
        System.out.println(" Join count: " + joinCount);
        System.out.println("===================================================");
    }
}
