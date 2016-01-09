package mainClasses;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import mainResourcesPackage.SoundEffect;

public class Ability
{
	protected static List<String>	descriptions				= new ArrayList<String>();
	protected static boolean[][]	elementalAttacksPossible	= new boolean[12][7];												// [element][ability]
	protected static int[][]		elementalAttackNumbers		= new int[12][3];
	protected static String[]		elementalAttacks			= new String[]
														{ "Ball", "Beam", "Shield", "Wall", "Spray", "Strike", "Pool" };

	// permanent variables of the ability
	protected String				name;																							// name of the ability
	protected int					points;																							// 1-10. AKA "level". Measures how powerful the ability is.
	protected double				cooldown;																						// Duration in which power doesn't work after using it. -1 = passive, 0 = no cooldown
	protected double				cost;																							// -1 = passive. Is a cost in mana, stamina or charge...depending on the power.
	protected double				costPerSecond;																					// applies to some abilities. Is a cost in mana, stamina or charge...depending on the power.
	protected int					range;																							// distance from user in which ability can be used. For some abilities - how far they go before stopping. -1 = not ranged, or only
																														// direction-aiming.
	protected double				areaRadius;																						// radius of area of effect of ability.
	protected boolean				instant;																						// Instant abilities don't aim, they immediately activate after a single click. Maintained abilities are always instant.
	protected boolean				maintainable;																					// Maintained abilities are instant, and require you to continue holding the button to use them (they're continuous abilities).
	protected boolean				stopsMovement;																					// Does the power stop the person from moving?
	protected boolean				onOff;																							// Is it an on/off ability.
	protected String				costType;																						// "none", "mana", "stamina", "charge" or "life". Abilities that use multiple don't exist, I think.

	// changing variables of the ability
	protected double				timeLeft;																						// how much time the ability has been on.
	protected double				cooldownLeft;																					// if cooldown is -1 (passive), then a cooldownLeft of 0 means the ability hasn't been initialized yet
	protected boolean				on;																								// For maintained and on/off abilities - whether or not the power is active.

	String[]			tags;																							// list of tags.
	// possible tags are:
	// offensive, projectile

	// EXAMPLES
	//
	// Fire Beam 7: name = "Beam <Fire>"; points = 7; cooldown = 0.5; cost = 0; costPerSecond = 5 / <fire damage>; range = 500*points; areaRadius = -1; instant = true; maintainable = true; stopsMovement = false; onOff = false; costType = "mana";

	// for special effects:
	double				targetEffect1				= -1,
								targetEffect2 = -1, targetEffect3 = -1;
	String				rangeType					= "";
	int					frameNum					= 0;

	List<SoundEffect>	sounds						= new ArrayList<SoundEffect>();

	@SuppressWarnings("unused")
	public void use(Environment env, Person user, Point target)
	{
		//to be written in child classes
	}

	@SuppressWarnings("unused")
	public void maintain(Environment env, Person user, Point target, double deltaTime)
	{
		//to be written in child classes
	}
	@SuppressWarnings("unused")
	public void updatePlayerTargeting(Environment env, Player player, Point target, double deltaTime)
	{
		//to be written in child classes
	}
	public Ability(String n, int p)
	{
		name = n;
		points = p;

		// default values. Ideally this code is useless.
		costPerSecond = -1;
		cooldown = -1;
		cost = -1;
		costPerSecond = -1;
		range = -1;
		areaRadius = -1;
		instant = false;
		maintainable = false;
		stopsMovement = false;
		onOff = false;
		costType = "";

		assignVariables();
		addSounds();
		cooldownLeft = 0;
		timeLeft = 0;
	}

	void assignVariables()
	{
		range = 600; // default range is 6 meters
		// Most stuff:
		switch (justName())
		{
		// passive abilities have a cost of -1! That's how you know!
		// cooldown -1 = passive, 1 = on/off, anything else is probably activated or maintained. It's IMPORTANT that on/off abilities have cooldown 1.
		case "Toughness III":
		case "Wound Regeneration II":
		case "Wound Regeneration I":
		case "Leg Muscles":
		case "Elemental Combat I":
		case "Elemental Combat II":
			cost = -1;
			costType = "none";
			cooldown = -1;
			range = -1;
			break;

		case "Ranged Explosion":
			cost = 3;
			costType = "mana";
			cooldown = 1;
			rangeType = "Ranged circular area";
			areaRadius = points * 50;
			break;
		case "Heal I":
			cost = 0;
			costPerSecond = 1;
			costType = "mana";
			cooldown = 0;
			range = 50 * points;
			rangeType = "Circle area";
			break;
		case "Strong Force Field":
			cooldown = Math.max(7 - points, 0.3);
			targetEffect1 = -1;
			targetEffect2 = 0; // length
			targetEffect3 = 0; // width
			range = 68;
			rangeType = "Exact range";
			cost = 4;
			costType = "mana";
			break;
		case "Flight I":
			costPerSecond = Math.max(5 - points, 0);
			costType = "stamina";
			cooldown = 1;
			cost = 0;
			break;
		case "Flight II":
			costPerSecond = 0.4;
			costType = "stamina";
			cooldown = 1;
			cost = 0;
			break;
		case "Telekinetic Flight":
			// costPerSecond = 0
			costType = "none";
			cooldown = 1;
			cost = 0;
			break;
		case "Pool":
			cost = Math.max(3 - 0.3 * points, 0.8); // reduced cost is that minus 1.5
			costPerSecond = 1;
			costType = "mana";
			cooldown = Math.max(3 - 0.3 * points, 0.3); // is used for creating the pool
			targetEffect1 = -1; // x grid position
			targetEffect2 = -1; // y grid position
			range = 600;
			rangeType = "Create in grid";
			break;
		case "Wall":
			cost = Math.max(3 - 0.3 * points, 0.8);
			costType = "mana";
			cooldown = Math.max(3 - 0.3 * points, 0.3); // is used for creating the wall
			costPerSecond = 1;
			targetEffect1 = -1; // x grid position
			targetEffect2 = -1; // y grid position
			range = 600;
			rangeType = "Create in grid";
			break;
		case "Force Shield":
			cost = 3;
			costType = "mana";
			cooldown = 1;
			targetEffect1 = -1;
			targetEffect2 = 0; // length
			targetEffect3 = 0; // width
			range = 68;
			rangeType = "Exact range";
			break;
		case "Blink":
			cost = 1 + (double) (points) / 3;
			costType = "mana";
			cooldown = 0.1 + (double) (points) / 4;
			targetEffect1 = 0;
			targetEffect2 = 2;
			targetEffect3 = 4;
			range = points * 100;
			rangeType = "Exact range"; // maybe change it to up-to range?
			break;
		case "Ball":
			cost = 5 / elementalAttackNumbers[getElementNum()][2];
			costType = "mana";
			cooldown = 5 / elementalAttackNumbers[getElementNum()][2];
			range = 80;
			rangeType = "Look";
			break;
		case "Beam":
			cost = 0;
			costPerSecond = 5 / elementalAttackNumbers[getElementNum()][2];
			costType = "mana";
			rangeType = "Exact range";
			cooldown = 0.5; // after stopping a beam attack, this is the cooldown to start a new one

			if (getElement().equals("Plant"))
				range = 80 * points;
			else
				range = 500 * points;
			break;
		case "Shield":
			cost = 2;
			costPerSecond = 0.3;
			costType = "mana";
			cooldown = 5;
			range = -1;
			rangeType = "Look";
			break;
		case "Ghost Mode I":
			cost = 2 * points;
			costType = "mana";
			costPerSecond = 0.3;
			cooldown = 5;
			range = -1;
			rangeType = "";
			break;

		// Unpowered abilities.
		case "Punch":
			cost = 1;
			costType = "stamina";
			cooldown = 0.55; // is actually 0.55 - Math.min(0.02*FITNESS, 0.15);
			range = 70; // is actually 1.15 * puncher's width? TODO
			rangeType = "Look";
			break;

		// Senses
		case "Sense Life":
		case "Sense Mana and Stamina":
		case "Sense Structure":
			cost = -1;
			costType = "none";
			cooldown = -1;
			range = (int) (50 * Math.pow(2, points));
			rangeType = "Circle area";
			break;
		case "Sense Powers":
			cost = 0;
			costType = "none";
			cooldown = 1;
			range = (int) (50 * Math.pow(2, points));
			rangeType = "Circle area";
			break;
		case "Sense Parahumans":
		case "Sense Element":
			cost = -1;
			costType = "none";
			cooldown = -1;
			range = (int) (50 * Math.pow(3, points));
			rangeType = "Circle area";
			break;
		default:
			Main.errorMessage("the Ability class doesn't implement the power: " + justName());
			break;
		}
		// StopsMovement, Maintainable
		switch (justName())
		{
		case "Ball":
		case "Beam":
		case "Heal I":
		case "Heal II":
		case "Absorb Armor":
		case "Wall":
		case "Pool":
			stopsMovement = false;
			maintainable = true;
			instant = true;
			onOff = false;
			break;
		case "Shield":
		case "Clairvoyance":
		case "Clairvoyance Charge":
		case "Move Wall":
		case "Escalating Scream":
		case "Explosion Charge":
			stopsMovement = true;
			maintainable = true;
			instant = true;
			onOff = false;
			break;
		case "Punch":
			stopsMovement = true;
			maintainable = false;
			instant = true;
			onOff = false;
			break;
		case "Flight I":
		case "Flight II":
		case "Telekinetic Flight":
		case "Ghost Mode I":
		case "Ghost Mode II":
			stopsMovement = false;
			maintainable = false;
			instant = true;
			onOff = true;
			break;
		case "Sense Powers":
			stopsMovement = false;
			maintainable = false;
			instant = true;
			onOff = false;
			break;
		default:
			stopsMovement = false;
			maintainable = false;
			instant = false;
			onOff = false;
			break;
		}

		// tags
		for (String s : Ability.descriptions)
			if (s.startsWith(justName()))
			{
				String text = getDescription(name);
				if (text.indexOf("\n") == -1)
					Main.errorMessage("ability class go to this line and solve this. name was "+name+" and text was: "+text);
				text = text.substring(text.indexOf("\n") + 1, text.indexOf("\n", text.indexOf("\n") + 1)); // skip first line, delete fluff and description
				tag(text);
			}
	}

	private void tag(String tagList)
	{
		// taglist is a string like "dangerous fire symbolic" which is parsed and will create a tag array of {"dangerous", "fire", "symbolic"}
		// make sure not to include double spaces!
		if (tagList.length() < 1)
		{
			tags = new String[0];
			return;
		}
		List<String> tags2 = new ArrayList<String>();
		String currTag = "";
		for (int i = 0; i < tagList.length(); i++)
		{
			if (tagList.charAt(i) != ' ')
				currTag += tagList.charAt(i);
			else
			{
				tags2.add(currTag);
				currTag = "";
			}
		}
		tags2.add(currTag);
		tags = new String[tags2.size()];
		for (int i = 0; i < tags2.size(); i++)
			tags[i] = tags2.get(i);
	}

	public boolean hasTag(String tag)
	{
		for (int i = 0; i < tags.length; i++)
			if (tags[i].equals(tag))
				return true;
		return false;
	}

	public String getTags()
	{
		String s = "";
		for (int i = 0; i < tags.length; i++)
			s += " "+tags[i];
		return s.substring(1);
	}
	
	public void stopAllSounds()
	{
		for (int i = 0; i < sounds.size(); i++)
			sounds.get(i).stop();
	}

	public void playSound(String s)
	{
		updateSound(s, true);
	}

	public void stopSound(String s)
	{
		updateSound(s, false);
	}

	public void updateSound(String s, boolean start)
	{
		boolean loop = false; // or just play once - play().
		SoundEffect sound = null;
		switch (s)
		{
		case "Beam":
			sound = sounds.get(0);
			loop = true;
			break;
		case "Punch hit":
			sound = sounds.get(Main.random.nextInt(3));
			loop = false;
			break;
		case "Punch miss":
			sound = sounds.get(Main.random.nextInt(3) + 4);
			loop = false;
			break;
		case "Blink success":
			sound = sounds.get(0);
			loop = false;
			break;
		case "Blink fail":
			sound = sounds.get(1);
			loop = false;
			break;
		default:
			Main.errorMessage("What's that? I can't hear you!");
			break;
		}
		if (!start)
			sound.stop();
		else if (loop)
			sound.loop();
		else
			sound.play();
	}

	public void addSounds()
	{
		switch (justName())
		{
		case "Beam":
			sounds.add(new SoundEffect("Beam.wav", "Beam"));
			break;
		case "Punch":
			sounds.add(new SoundEffect("punch_1.wav", "Punch"));
			sounds.add(new SoundEffect("punch_2.wav", "Punch"));
			sounds.add(new SoundEffect("punch_3.wav", "Punch"));
			sounds.add(new SoundEffect("punch-miss_1.wav", "Punch"));
			sounds.add(new SoundEffect("punch-miss_2.wav", "Punch"));
			sounds.add(new SoundEffect("punch-miss_3.wav", "Punch"));
			sounds.add(new SoundEffect("punch-miss_4.wav", "Punch"));
			break;
		case "Blink":
			sounds.add(new SoundEffect("Blink-success.wav", "Blink"));
			sounds.add(new SoundEffect("Blink-fail.wav", "Blink"));
			break;
		default:
			break;
		}
	}

	public static String getName(String text)
	{
		// no element, no description or fluff
		return text.substring(0, text.indexOf("(") - 1);
		// Will give an error message if there is no "(" in the ability's name (or text), so when that happens insert a printing function here to see where you forgot a newline or something
	}

	public String getElement()
	{
		if (name.contains("<"))
			return name.substring(name.indexOf("<") + 1, name.indexOf(">"));
		return "NONE";
	}

	public int getElementNum()
	{
		if (name.contains("<"))
			return EP.toInt(getElement());
		return -1;
	}

	public String justName()
	{
		if (name.contains("<"))
			return name.substring(0, name.indexOf("<") - 1);
		return name;
	}

	public static String getDescription(String name)
	{
		// name must not contain any numbers or elements
		// this method's returned string contains <E>
		if (name.contains("<"))
		{
			for (int i = 0; i < descriptions.size(); i++)
				if (descriptions.get(i).substring(0, descriptions.get(i).indexOf('(') - 1).equals(name.substring(0, name.indexOf("<") - 1)))
					return descriptions.get(i);
		} else
			for (int i = 0; i < descriptions.size(); i++)
				if (descriptions.get(i).substring(0, descriptions.get(i).indexOf('(') - 1).equals(name))
					return descriptions.get(i);
		return "String not found in abilities: " + name;

	}

	public String niceName()
	{ // turns "Ball <Fire>" into "Fire Ball"
		if (name.contains("<"))
			return name.substring(name.indexOf("<") + 1, name.indexOf(">")) + " " + name.substring(0, name.indexOf("<") - 1);
		return name;
	}

	public String getFluff()
	{
		return Ability.getFluff(name);
	}

	public static String getFluff(String ability)
	{
		String realName = ability;
		String element = "0";
		if (ability.contains("<"))
		{
			realName = ability.substring(0, ability.indexOf("<") - 1);
			element = ability.substring(ability.indexOf("<") + 1, ability.indexOf(">")).toLowerCase();
		}
		String text = getDescription(ability);
		if (text == null)
			return "<no ability found with the name \"" + realName + "\".>";

		// flesh stuff :)
		if (element.equals("flesh"))
		{
			if (realName.contains("Combat"))
				element = "flesh, bone and blood";
			else if (realName.contains("Beam"))
				element = "blood";
			else if (realName.contains("Ball"))
				element = "meat";
			else if (realName.contains("Shield"))
				element = "meat";
			else if (realName.contains("Spray"))
				element = "bones";
			else if (realName.contains("Strike"))
				element = "skeletally-enhanced";
			else if (realName.contains("Sense"))
				text = "Sense Element (Passive) <Flesh>\nSense blood pools, bone walls, meat shields, and people with Flesh powers.\nSee silhouettes of capes with your elemental power, walls/pools of your element or creatures under your elementís effect, and know how hurt they are. The range is 3^Level. Sense Element <Earth> allows you to also have Sense Structure! Yup!";
			else if (realName.contains("Strike"))
				element = "skeletally-enhanced";
			else if (realName.contains("Fists"))
				element = "strengthened bones";
			else if (realName.contains("Chemtrail"))
				element = "bloody";
			else if (realName.contains("Armor")) // It's a flesh armor, that's what I decided
				element = "flesh";
			else if (realName.contains("Trail"))
				element = "bloody";
			else if (realName.contains("Teletrail"))
				element = "flesh";
			else if (realName.contains("Teleport"))
				element = "bones, blood or meat (that aren't part of a human being)";
			else if (realName.contains("Theft"))
				element = "Flesh-related";
			else if (realName.contains("Summoning"))
				element = "flesh";
			else if (realName.contains("Explosions"))
				element = "flesh and gore! :D";
			else if (realName.contains("Specialty"))
				element = "flesh (as a power)";
			else if (realName.contains("Reshape"))
				element = "bones, blood and meat";
			else if (realName.contains("Pool"))
				element = "blood";
			else if (realName.contains("Melt"))
				text = "Melt (Activated) <Flesh>\nMelt a bone wall into a blood pool. Because powers don't always make sense.\nTarget an <Element> wall. Destroy it and create an <Element> pool instead.\nCost: 0 Mana.";
			else if (realName.contains("Launch"))
				text = "Launch Wall (Activated) <Flesh>\nTransform a bone wall into a meat ball through sheer force of will, and then throw it at a target.\nTarget a wall and then a creature. The wall immediately becomes an <Element> Ball and is thrown towards the target.\nCost: 2 Mana.";
			else if (realName.contains("Wall")) // important that this is after "Launch"!
				element = "bone";
			else
				element = "BUG (Flesh) BUG";
		}
		text = text.substring(text.indexOf("\n") + 1); // skip name and type
		text = text.substring(text.indexOf("\n") + 1, text.indexOf("\n", text.indexOf("\n") + 1)); // skip tags, delete description
		text = text.replace("<e>", element);
		switch (element.charAt(0))
		// Add "a"/"an" depending on if element starts with a vowel. This part of method might need to move to EP.
		{
		case 'a':
		case 'e':
		case 'i':
		case 'o':
		case 'u':
			text = text.replace("<an e>", "an " + element);
			break;
		default:
			text = text.replace("<an e>", "a " + element);
			break;
		}
		return text;
	}

	static List<Ability> ECAbilities(int elementNum, int rank, int points)
	{
		// rank 1 = Elemental Combat I
		// rank 2 = Elemental Combat II
		// rank 3 = Charged Elemental Combat
		List<Ability> addedAbilities = new ArrayList<Ability>();

		switch (rank)
		{
		case 1: // Ball, Beam, Shield
			for (int i = 0; i < 3; i++)
				if (elementalAttacksPossible[elementNum][i])
					addedAbilities.add(new Ability(elementalAttacks[i] + " <" + EP.elementList[elementNum] + ">", points));
			break;
		case 2: // All (Ball, Beam, Shield, Wall, Pool, Spray, Strike) not necessarily in that order
			for (int i = 0; i < 7; i++)
				if (elementalAttacksPossible[elementNum][i])
					addedAbilities.add(new Ability(elementalAttacks[i] + " <" + EP.elementList[elementNum] + ">", points));
			break;
		case 3: // Charged Beam, Charged Ball
			for (int i = 0; i < 2; i++)
				if (elementalAttacksPossible[elementNum][i])
					addedAbilities.add(new Ability("Charged " + elementalAttacks[i] + " <" + EP.elementList[elementNum] + ">", points));
			break;
		default:
			Main.errorMessage("Unknown rank: " + rank);
			break;
		}

		return addedAbilities;
	}

	public static void initializeDescriptions()
	{
		try
		{
			// combinations
			BufferedReader in = new BufferedReader(new InputStreamReader(Ability.class.getResourceAsStream("abilities.txt"), "UTF-8"));
			if (!in.ready())
			{
				Main.errorMessage("EMPTY FILE - ABILITIES");
				in.close();
				return;
			}
			in.read();
			while (in.ready())
			{
				String s = "";
				String currLine = in.readLine();
				while (currLine != null && !currLine.equals(""))
				{
					s += currLine + "\n";
					currLine = in.readLine();
				}
				currLine = in.readLine(); // extra paragraph break
				s = s.substring(0, s.length() - 1); // remove final paragraph break
				descriptions.add(s);
			}

			// elemental attacks possible
			in = new BufferedReader(new InputStreamReader(Ability.class.getResourceAsStream("elementalCombatPossibilities.csv"), "UTF-8"));
			if (!in.ready())
			{
				Main.errorMessage("EMPTY FILE - ELEMENTAL COMBAT ATTACKS");
				in.close();
				return;
			}
			String line = "";
			int i = 0;
			while (in.ready())
			{
				line = in.readLine();
				int j = 0;
				for (int k = 0; k < line.length(); k++)
					switch (line.charAt(k))
					{
					case 'X':
						elementalAttacksPossible[i][j] = true;
						j++;
						break;
					case 'O':
						elementalAttacksPossible[i][j] = false;
						j++;
						break;
					case ',':
						break;
					default: // Damage and stuff
						elementalAttackNumbers[i][j - 7] = Integer.parseInt("" + line.charAt(k));
						j++;
						break;
					}
				i++;
			}
			in.close();
		} catch (IOException e)
		{
			e.printStackTrace();
			Main.errorMessage("(there was a bug, I think)");
		}
	}

	public String toString()
	{
		return name + " [" + points + "]";
	}

	static Comparator<Ability> pointsThenAlphabetical = new Comparator<Ability>()
	{
		public int compare(Ability a1, Ability a2)
		{
			if (a1.points != a2.points)
				return Integer.compare(a1.points, a2.points);
			else
				return a1.name.compareTo(a2.name);
		}
	};
}