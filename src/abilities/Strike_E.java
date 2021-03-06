package abilities;

import java.awt.Point;

import mainClasses.Ability;
import mainClasses.Environment;
import mainClasses.Person;
import mainClasses.Player;
import mainResourcesPackage.SoundEffect;

public class Strike_E extends Ability
{

	public Strike_E(String elementName, int p)
	{
		super("Strike <" + elementName + ">", p);
		costType = Ability.CostType.MANA;
		instant = true;

		sounds.add(new SoundEffect(elementName + " Smash.wav"));
	}

	public void updateStats()
	{
		cost = 3 / elementalAttackNumbers[elementNum][2];
		damage = 0.6 * LEVEL * Ability.elementalAttackNumbers[elementNum][0];
		pushback = 0.6 * LEVEL * Ability.elementalAttackNumbers[elementNum][1] + 1;
		
	}

	public void use(Environment env, Person user, Point target)
	{
		if (!on && user.mana >= cost)
		{
			user.mana -= cost;
			user.punchAffectingAbilities.add(this);
			on = true;
		}
	}

	public void turnOff()
	{
		on = false;
	}

	public void disable(Environment env, Person user)
	{
		disabled = true;
		on = false;
	}

	public void maintain(Environment env, Person user, Point target, double deltaTime)
	{
		;
	}

	public void updatePlayerTargeting(Environment env, Player player, Point target, double deltaTime)
	{
		;
	}
}
