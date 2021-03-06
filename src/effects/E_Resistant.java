package effects;

import mainClasses.Ability;
import mainClasses.Effect;
import mainClasses.Person;

public class E_Resistant extends Effect
{
	public String element;

	public E_Resistant(String element1, int strength1, Ability CA)
	{
		super(element1 + " Resistant", -1, strength1, CA);
		stackable = true;
		removeOnDeath = false;
		element = element1;
		removable = false;
	}

	public Effect clone()
	{
		E_Resistant e = new E_Resistant(this.element, (int) this.strength, this.creatorAbility);
		e.timeLeft = this.timeLeft;
		e.strength = this.strength;
		e.animFrame = this.animFrame;
		e.stackable = this.stackable;
		e.removeOnDeath = this.removeOnDeath;
		e.element = this.element;
		return e;
	}

	@Override
	public void apply(Person target)
	{
		;
	}

	@Override
	public void unapply(Person target)
	{
		;
	}

	public void nextFrame(int frameNum)
	{
		;
	}
}
