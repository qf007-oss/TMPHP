package THUIM;


import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a UtilityList as used by the HUI-Miner algorithm.
 *
 * @see PTHUIMAlgo
 * @see Element
 * @author Philippe Fournier-Viger
 */
public class UtilityList {
	 Integer item;  // the item
	 long sumIutils = 0;  // the sum of item utilities
	 long sumRutils = 0;  // the sum of remaining utilities
	 List<Element> elements = new ArrayList<Element>();  // the elements

	int largestPeriodicity = 0;
	int smallestPeriodicity = Integer.MAX_VALUE;

	/**
	 * Get the periodicity of this pattern.
	 * @return the periodicity
	 */
	public int getLargestPeriodicity() {
		return largestPeriodicity;
	}


	/**
	 * Constructor.
	 * @param item the item that is used for this utility list
	 */
	public UtilityList(Integer item){
		this.item = item;
	}
	
	/**
	 * Method to add an element to this utility list and update the sums at the same time.
	 */
	public void addElement(Element element){
		sumIutils += element.iutils;
		sumRutils += element.rutils;
		elements.add(element);
	}
	
	/**
	 * Get the support of the itemset represented by this utility-list
	 * @return the support as a number of trnsactions
	 */
	public int getSupport() {
		return elements.size();
	}
}
