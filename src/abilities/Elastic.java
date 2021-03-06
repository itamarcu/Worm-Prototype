package abilities;

import java.awt.Point;

import mainClasses.Environment;
import mainClasses.Person;

public class Elastic extends _PassiveAbility
{
	double bonusStrength;

	public double minimumVelocityPow2 = Math.pow(500, 2);

	public Elastic(int p)
	{
		super("Elastic", p);
	}

	public void updateStats()
	{
		damage = LEVEL * 1;
		pushback = LEVEL * 2;
		bonusStrength = LEVEL;
	}

	public void use(Environment env, Person user, Point target)
	{
		on = !on;
		int val = on ? 1 : -1;
		user.STRENGTH += bonusStrength * val;
	}
}
