package mainClasses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Valuable class! Generates a name for a parahuman according to their DNA (which almost always reflects their powers).
 * 
 * @author Itamar
 *
 */
public class NameGenerator
{
	public static List<String> nouns, verbs, adjs, males, females, pretitles, posttitles;
	public static List<List<String>> elementRelatedNouns = new ArrayList<List<String>>(); // 0 - 31 elements, 32 general
	public static List<List<String>> elementRelatedVerbs = new ArrayList<List<String>>(); // 0 - 31 elements, 32 general
	public static List<List<String>> elementRelatedAdjs = new ArrayList<List<String>>(); // 0 - 31 elements, 32 general
	public static Random random = new Random();

	/**
	 * If the DNA is empty, will return a "muggle" name, using {@link #firstName()}. Else - will find a mainElement (element with most points) and a secondaryElement (one with second most points). Then, will use them and the {@link #noun(String)},
	 * {@link #verb(String)}, {@link #adj(String)}, {@link #posttitles}, {@link #pretitles} lists to generate a random name using the following templates:
	 * <ul>
	 * <li>Noun Noun (double chance) = Night Hag</li>
	 * <li>Nounnoun (double chance) = Laserdream</li>
	 * <li>(The) Noun (50% of having the "The ") = The Custodian / Rune</li>
	 * <li>Title Noun = Miss Militia</li>
	 * <li>Noun Title = Glory Girl</li>
	 * <li>Verber = Screamer</li>
	 * <li>Verbnoun = Shatterbird</li>
	 * <li>Adjective Noun = Gentle Giant</li>
	 * <li>Adjective Title = Gray Boy</li>
	 * <li>Noun the Adjective = Crane the Harmonious</li>
	 * </ul>
	 * 
	 * @param DNA
	 * @return
	 */
	public static String generate(List<EP> DNA)
	{
		String name = null;
		if (!DNA.isEmpty())
		{
			EP mainEP = null;
			EP secondaryEP = null;
			int mainElementAmount = -1;
			for (EP ep : DNA)
				if (ep.points > mainElementAmount)
				{
					secondaryEP = mainEP;
					mainEP = ep;
					mainElementAmount = ep.points;
				}
			String mainElement = EP.elementList[mainEP.elementNum];
			String secondaryElement = secondaryEP == null ? "general" : EP.elementList[secondaryEP.elementNum];

			switch (random.nextInt(12))
			{
			case 6:
			case 0:// Noun Noun
				name = noun(secondaryElement) + " " + noun(mainElement);
				break;
			case 5:
			case 1:// Nounnoun
				do
					name = noun(secondaryElement) + noun(mainElement).toLowerCase();
				while (name.length() > 11);
				break;
			case 2:// (The) Noun
				name = noun(mainElement);
				if (random.nextBoolean())
					name = "The " + name;
				break;
			case 3:// Title Noun
				name = pretitles.get(random.nextInt(pretitles.size())) + " " + noun(mainElement); // TODO check gender
				break;
			case 4:// Noun Title
				name = noun(mainElement) + " " + posttitles.get(random.nextInt(posttitles.size()));
				break;
			case 7:// Verber
				name = add_er(verb(mainElement));
				break;
			case 8: // Verbnoun
				if (random.nextBoolean())
					name = verb(mainElement) + noun(secondaryElement).toLowerCase();
				else
					do
						name = verb(secondaryElement) + noun(mainElement).toLowerCase();
					while (name.length() > 11);
				break;
			case 9: // Adjective Noun
				name = adj(secondaryElement) + " " + noun(mainElement);
				break;
			case 10: // Adjective
				name = adj(mainElement) + " " + posttitles.get(random.nextInt(posttitles.size()));
				break;
			case 11: // Noun the Adjective
				name = noun(mainElement) + " the " + adj(secondaryElement);
				break;
			default:
				name = "------------";
				break;
			}
		}
		else
		{
			// Muggle names
			String firstName = firstName();
			name = firstName;
		}

		// Small chance of coolness
		char[] cs = name.toCharArray();
		for (int i = 0; i < cs.length; i++)
			if (random.nextInt(100) == 0)
			{
				// 1 in 100 chance per letter of that letter becoming cooler: C -> K, I -> EE.
				// For example, "Majestik" or "Peenk Panda"
				if (cs[i] == 'c' && (i + 1 >= cs.length || cs[i + 1] != 'h' || cs[i + 1] != 'e')) // letter is c and not followed by H or E
					cs[i] = 'k'; // c -> k
				if (cs[i] == 'i' && i + 1 < cs.length)
				{
					cs[i] = 'e'; //
					cs[i + 1] = 'e'; // i -> ee
				}
			}

		return name;
	}

	public static String add_er(String str)
	{
		List<Character> vowels = Arrays.asList('a', 'e', 'i', 'o', 'u');
		int lng = str.length();
		switch (str.charAt(lng - 1))
		{
		case 'y':
			if (!vowels.contains(str.charAt(lng - 2)))
				return str.substring(0, lng - 1) + "ier";
			break;
		case 'e':
			return str + "r";
		default:
			break;
		}
		if (str.charAt(lng - 2) != 'w' && str.charAt(lng - 2) != 'r' && vowels.contains(str.charAt(lng - 2)) && !vowels.contains(str.charAt(lng - 1)) && !vowels.contains(str.charAt(lng - 3))) // covers most cases
			return str + str.charAt(lng - 1) + "er";
		return str + "er";
	}

	public static String noun(String element)
	{
		int num = -1;
		if (element.equals("general"))
			num = 32;
		else
			num = EP.toInt(element);
		if (random.nextInt(8) == 0)
			num = 32;
		return elementRelatedNouns.get(num).get(random.nextInt(elementRelatedNouns.get(num).size()));
	}

	public static String verb(String element)
	{
		int num = -1;
		if (element.equals("general"))
			num = 32;
		else
			num = EP.toInt(element);
		if (random.nextInt(8) == 0)
			num = 32;
		return elementRelatedVerbs.get(num).get(random.nextInt(elementRelatedVerbs.get(num).size()));
	}

	public static String adj(String element)
	{
		int num = -1;
		if (element.equals("general"))
			num = 32;
		else
			num = EP.toInt(element);
		if (random.nextInt(3) == 0) // 33% of any adjective being a general one - because they're simply too good and plentiful
			num = 32;
		return elementRelatedAdjs.get(num).get(random.nextInt(elementRelatedAdjs.get(num).size()));
	}

	public static String firstName()
	{
		if (random.nextBoolean())
			return females.get(random.nextInt(males.size()));
		else
			return males.get(random.nextInt(males.size()));
	}

	public static String alliterative(List<String> wordsToPickFrom, String toMatch)
	{
		List<String> suitableWords = new ArrayList<String>();
		for (String s : wordsToPickFrom)
			if (s.startsWith(toMatch))
				suitableWords.add(s);
		if (suitableWords.isEmpty())
			return "Problematic";
		return suitableWords.get(random.nextInt(suitableWords.size()));
	}

	public static void initialize()
	{
		nouns = new ArrayList<String>();
		verbs = new ArrayList<String>();
		adjs = new ArrayList<String>();
		males = new ArrayList<String>();
		females = new ArrayList<String>();
		pretitles = new ArrayList<String>();
		posttitles = new ArrayList<String>();
		for (int i = 0; i < 33 + 1; i++)
			elementRelatedNouns.add(new ArrayList<String>());
		for (int i = 0; i < 33 + 1; i++)
			elementRelatedVerbs.add(new ArrayList<String>());
		for (int i = 0; i < 33 + 1; i++)
			elementRelatedAdjs.add(new ArrayList<String>());

		try
		{
			// nouns
			BufferedReader in = new BufferedReader(new InputStreamReader(PowerGenerator.class.getResourceAsStream("nouns.txt"), "UTF-8"));
			while (in.ready())
			{
				String line = in.readLine();
				try
				{
					String name = line.substring(0, line.indexOf('-'));
					name = capitalize(name);
					for (int i = 0; i < EP.elementList.length; i++)
						if (line.contains(EP.elementList[i].toLowerCase()))
							elementRelatedNouns.get(i).add(name);
					if (line.contains("general"))
						elementRelatedNouns.get(32).add(name);
					nouns.add(name);
				}
				catch (Exception e)
				{
					// System.out.println("missing hyphen in: " + line);
				}
			}
			in.close();

			// verbs
			in = new BufferedReader(new InputStreamReader(PowerGenerator.class.getResourceAsStream("verbs.txt"), "UTF-8"));
			while (in.ready())
			{
				String line = in.readLine();
				if (line == null)
					MAIN.errorMessage("FindBugs warned me about this! Seriously!");
				else
					try
					{
						String name = line.substring(0, line.indexOf('-'));
						name = capitalize(name);
						for (int i = 0; i < EP.elementList.length; i++)
							if (line.contains(EP.elementList[i].toLowerCase()))
								elementRelatedVerbs.get(i).add(name);
						if (line.contains("general"))
							elementRelatedVerbs.get(32).add(name);
						verbs.add(name);
					}
					catch (Exception e)
					{
						System.out.println("missing hyphen in: " + line);
					}
			}
			in.close();

			// adjectives
			in = new BufferedReader(new InputStreamReader(PowerGenerator.class.getResourceAsStream("adjectives.txt"), "UTF-8"));
			while (in.ready())
			{
				String line = in.readLine();
				try
				{
					String name = line.substring(0, line.indexOf('-'));
					name = capitalize(name);
					for (int i = 0; i < EP.elementList.length; i++)
						if (line.contains(EP.elementList[i].toLowerCase()))
							elementRelatedAdjs.get(i).add(name);
					if (line.contains("general"))
						elementRelatedAdjs.get(32).add(name);
					adjs.add(name);
				}
				catch (Exception e)
				{
					// System.out.println("missing hyphen in: " + line);
				}
			}
			in.close();

			// males
			in = new BufferedReader(new InputStreamReader(PowerGenerator.class.getResourceAsStream("males.txt"), "UTF-8"));
			while (in.ready())
				males.add(capitalize(in.readLine()));
			in.close();

			// females
			in = new BufferedReader(new InputStreamReader(PowerGenerator.class.getResourceAsStream("females.txt"), "UTF-8"));
			while (in.ready())
				females.add(capitalize(in.readLine()));

			in.close();

			// titles
			pretitles = Arrays.asList("Lord", "Lady", "Mr", "Doctor", "Miss", "Captain", "Admiral", "Sir");
			posttitles = Arrays.asList("Man", "Woman", "Dude", "Mistress", "Master", "Girl", "Kid", "Boy");

		}
		catch (IOException e)
		{
			MAIN.errorMessage("something wrong in name generation");
			e.printStackTrace();
		}
	}

	public static String capitalize(String str)
	{
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}
}
