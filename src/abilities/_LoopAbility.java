package abilities;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import mainClasses.Ability;
import mainClasses.Environment;
import mainClasses.MAIN;
import mainClasses.Methods;
import mainClasses.Person;
import mainClasses.Player;

/**
 * An ability that loops back one or several targets in time, in state and/or position.
 * 
 * @author Itamar
 *
 */
public class _LoopAbility extends Ability
{
	public boolean state;
	public boolean position;

	public enum Targeting
	{
		SELF, TARGETED, AREA
	};

	public Targeting targeting;

	public _LoopAbility(String name, int p, Targeting targeting1)
	{
		super(name, p);
		targeting = targeting1;
	}

	/**
	 * Like getTarget but with possibly many targets, and depends on {@link #targeting}
	 * 
	 * @param env
	 * @param user
	 * @param target
	 * @return List of possible targets
	 */
	public List<Person> getTargets(Environment env, Person user, Point target)
	{
		List<Person> targets = new ArrayList<Person>();
		switch (targeting)
		{
		case SELF:
			targets.add(user);
			break;
		case TARGETED:
			double shortestDistPow2 = maxDistFromTargetedPoint * maxDistFromTargetedPoint;
			for (Person p : env.people)
			{
				double distPow2 = Methods.DistancePow2(p.Point(), target);
				if (distPow2 < shortestDistPow2)
				{
					shortestDistPow2 = distPow2;
					targets.clear();
					targets.add(p);
				}
			}
			break;
		case AREA:
			for (Person p : env.people)
			{
				double distPow2 = Methods.DistancePow2(p.Point(), target);
				if (distPow2 < radius * radius)
				{
					targets.add(p);
				}
			}
			break;
		default:
			MAIN.errorMessage("I'd get a chicken or something, I don't give a shit about chickens");
			break;
		}
		return targets;
	}

	/**
	 * Applies the loop on every target.
	 */
	public void use(Environment env, Person user, Point target)
	{
		List<Person> targets = getTargets(env, user, target);
		if (targets.isEmpty())
			return;
		if (user.mana >= cost && cooldownLeft <= 0)
		{
			user.mana -= cost;
			cooldownLeft = cooldown;

			for (Person p : targets)
				p.loop((int) duration, state, position);
		}
	}

	public void disable(Environment env, Person user)
	{
		disabled = true;
	}

	public void updatePlayerTargeting(Environment env, Player player, Point target, double deltaTime)
	{
		if (targeting == Targeting.SELF)
			player.target = player.Point();
		else
			player.target = target;
		player.aimType = Player.AimType.LOOP;
	}
}
