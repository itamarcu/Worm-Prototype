package mainClasses;

public class Effect implements Cloneable
{
	public double duration; // -1 = forever
	public double timeLeft; // -1 = forever
	public String name; // can be an int or a short, honestly, but then the programming would be difficulter
	public double strength;
	public int animFrame;
	public Ability creatorAbility; // to avoid same ability giving same effect multiple times to same person
	public boolean timeAffecting;
	public boolean removable;
	public boolean removeOnDeath;
	public boolean stackable;

	public Effect(String name1, double duration1, double strength1, Ability creatorAbility1)
	{
		creatorAbility = creatorAbility1;
		name = name1;
		duration = duration1;
		timeLeft = duration;
		strength = strength1;
		timeAffecting = false;
		removable = true;
		animFrame = 0;
	}

	public Effect()
	{

	}

	public Effect clone()
	{
		MAIN.errorMessage("Pardon me, but can you mayhaps fix this shit wanker?");
		return new Effect(null, -1, -1, null);
	}

	@SuppressWarnings("unused")
	public void apply(Person target)
	{
		MAIN.errorMessage("Error message. 1234567");
	}

	@SuppressWarnings("unused")
	public void unapply(Person target)
	{
		MAIN.errorMessage("String literal out of exception campaign");
	}

	public void update(Person target, double deltaTime)
	{
		timeLeft -= deltaTime;
		if (timeLeft < 0)
		{
			unapply(target);
		}
	}

	@SuppressWarnings("unused")
	public void nextFrame(int frameNum)
	{
		MAIN.errorMessage("Please don't kill me! I have a dog and kids!");
	}
}