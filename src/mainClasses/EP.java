package mainClasses;

import java.awt.Color;

/**
 * EP = Element Points. Every EP is the equivalent of "6 Fire Points" or something like that. The sum of all EPs of a person is 10. If they have powers.
 * 
 * @author Itamar
 *
 */
public class EP
{
	public int elementNum;
	public int points;

	/**
	 * 
	 * @param e
	 *            element index, e.g. 10 for Earth
	 * @param p
	 *            number of points / level
	 */
	public EP(int e, int p)
	{
		elementNum = e;
		points = p;
	}

	/**
	 * 
	 * @param e
	 *            element name, e.g. "Strong"
	 * @param p
	 *            number of points / level
	 */
	public EP(String e, int p)
	{
		elementNum = toInt(e);
		points = p;
	}

	/**
	 * This other version of toString is in uppercase if it's a main element, and lowercase if it's minor. Easier to read that way, for me.
	 * 
	 * @return string describing this EP: NAME points or name points
	 */
	public String toString2()
	{
		if (points < 4)
			return "" + elementList[elementNum].toLowerCase() + " " + points;
		return "" + elementList[elementNum].toUpperCase() + " " + points;
	}

	/**
	 * 
	 * @return string describing this EP: Name points
	 */
	public String toString()
	{
		return "" + elementList[elementNum] + " " + points;
	}

	/**
	 * 
	 * @param element
	 *            name of the element
	 * @return damage type of element
	 */
	public static int damageType(String element)
	{
		switch (element)
		{
		case "Water":
		case "Wind":
		case "Metal":
		case "Earth":
		case "Plant":
			return 0; // impact
		case "Ice":
		case "Flesh":
			return 1; // stab
		case "Fire":
		case "Lava":
			return 2; // fire
		case "Acid":
			return 3; // acid
		case "Electricity":
		case "Energy":
		case "Force Field":
			return 4; // shock
		default:
			MAIN.errorMessage("5555: Unknown element! " + element);
			return -1;
		}
	}

	/**
	 * 
	 * @param damageType
	 * @return name of damage type
	 */
	public static String nameOfDamageType(int damageTypeNum)
	{
		switch (damageTypeNum)
		{
		case 0:
			return "Impact";
		case 1:
			return "Stab";
		case 2:
			return "Burn";
		case 3:
			return "Acid";
		case 4:
			return "Shock";
		case 9:
			return "Phantom";
		default:
			MAIN.errorMessage("GuillotineTit: Unknown damage type! " + damageTypeNum);
			return "WTF";
		}
	}

	/**
	 * 
	 * @param eNum
	 *            number of element. -1 = blunt. 12 = force field.
	 * @return damage type that this element does
	 */
	public static int damageType(int eNum)
	{
		switch (eNum)
		{
		case -2: // outer bound walls
			return -2; // spectral
		case -1: 
		case 1:
		case 2:
		case 4:
		case 10:
		case 11:
			return 0; // impact
		case 5:
		case 9:
			return 1; //stab
		case 0:
		case 8:
			return 2; //burn
		case 7:
			return 3; //acid
		case 3:
		case 6:
		case 12: // force field - applies to bubbles
			return 4; // shock
		default:
			MAIN.errorMessage("2112: Unknown damage type! " + eNum);
			return -1;
		}
	}

	/**
	 * 
	 * @param elementName
	 * @return index of that element in the element list; elementNum
	 */
	public static int toInt(String elementName)
	{
		for (int i = 0; i < elementList.length; i++)
			if (elementList[i].equals(elementName))
				return i;
		return -1;
	}

	/**
	 * List of all of the 32 elements. Note that "element" does not only include the first 12 elements (fire, water....plant), which are the "elemental elements".
	 */
	public static String[] elementList =
	{ "Fire", "Water", "Wind", "Electricity", "Metal", "Ice", "Energy", "Acid", "Lava", "Flesh", "Earth", "Plant", "Sense", "Strong", "Regenerate", "Flight", "Dexterity", "Armor", "Movement",
			"Teleport", "Ghost", "Force Field", "Time", "Loop", "Power", "Steal", "Audiovisual", "Summon", "Explosion", "Control", "Buff", "Charge" };
	/**
	 * Colors that identify each element. Note that the Ghost element has some transparency to it.
	 */
	public static Color[] elementColors = new Color[]
	{ Color.decode("#FF6A00"), Color.decode("#0094FF"), Color.decode("#CDE8FF"), Color.decode("#FFD800"), Color.decode("#999999"), Color.decode("#84FFFF"), Color.decode("#E751FF"),
			Color.decode("#A8A30D"), Color.decode("#D32B00"), Color.decode("#FF75AE"), Color.decode("#8C2F14"), Color.decode("#5DAE00"), Color.decode("#91C6FF"), Color.decode("#4F2472"),
			Color.decode("#156B08"), Color.decode("#D1CDFF"), Color.decode("#00E493"), Color.decode("#0800FF"), Color.decode("#FFF9A8"), Color.decode("#1ECAFF"), new Color(224, 224, 224, 120),
			Color.decode("#C6FF7C"), Color.decode("#A7C841"), Color.decode("#6D6B08"), Color.decode("#693F59"), Color.decode("#404E74"), Color.decode("#FFE2EC"), Color.decode("#8131C6"),
			Color.decode("#E57600"), Color.decode("#FFC97F"), Color.decode("#8FFFC2"), Color.decode("#FF9F00") };
}
