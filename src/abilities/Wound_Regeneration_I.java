package abilities;

import java.awt.Point;

import mainClasses.Environment;
import mainClasses.Person;

public class Wound_Regeneration_I extends _PassiveAbility
{

	public Wound_Regeneration_I(int p)
	{
		super("Wound Regeneration I", p);
	}
	
	public void updateStats()
	{
		amount = 1 * LEVEL;
	}

	public void use(Environment env, Person user, Point target)
	{
		int val = on ? -1 : 1;
		user.lifeRegen += val * amount;
		on = !on;
	}
}
