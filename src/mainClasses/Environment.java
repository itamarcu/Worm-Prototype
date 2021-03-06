package mainClasses;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import abilities.Charge;
import abilities.Elastic;
import abilities.Explosion_Resistance;
import abilities.Portals;
import effects.Burning;
import effects.E_Resistant;
import effects.Tangled;
import mainResourcesPackage.SoundEffect;
import pathfinding.Path;
import pathfinding.PathFinder;
import pathfinding.ProcGenPathFinder;
import pathfinding.ProceduralGenerationMap;

public class Environment
{
	public final static int squareSize = 96;
	public final int amountOfElements;
	public final double TAU = Math.PI * 2;
	public final int numOfClouds = 0;
	public final int minCloudHeight = 60, maxCloudHeight = 400;
	public final static double[] floorFriction = new double[]
	{ 0.65, 0.55, 0.7, 0.8, 0.55, 0.65, 0.65 };
	public final static double[] poolFriction = new double[]
	{ -1, 0.3, -1, -1, 0.8, 0.2, -1, 0.6, 0.7, 0.3, 0.8, 0.6 }; // depending on pool type
	public final static double[] wallFriction = new double[]
	{ -1, 0.3, -1, -1, 0.8, 0.2, -1, 0.6, 0.7, 0.3, 0.8, 0.6, 0.75 }; // depending on wall type
	public boolean devMode = false;
	public boolean showDamageNumbers = true;
	public Point windDirection;
	public double shadowX, shadowY;

	// All of these shouldn't be ints, they range from -1 to 12 or -1 to 100. :/
	public int width, height, widthPixels, heightPixels;
	public int[][] floorTypes; // -1 = no floor. 0 = ground.
	public int[][] wallTypes; // 2D array of wall types. Types are equal to the wall's element. -1 = no wall.
	public int[][] wallHealths; // 2D array of wall healths. -1 = no wall. 100 = full health wall.
	public int[][] poolTypes; // ditto, for pools
	public int[][] poolHealths; // ditto, for pools

	public BufferedImage[][] poolImages; // cropped images, to join the pool corners
	public int[][] cornerCracks;
	public int[][][] wCornerStyles; // x, y, element; corners on the *UP-LEFT* corner of the corresponding square. +1 = +90 degrees clockwise
	public int[][][] pCornerStyles; // x, y, element; the int means both the shape and its rotation
	public int[][][] pCornerTransparencies;

	public List<VisualEffect> visualEffects;
	public List<ArcForceField> AFFs; // Arc Force Fields
	public List<Person> people;
	public List<Ball> balls;
	public List<Debris> debris;
	public List<UIText> uitexts;
	public List<ForceField> FFs; // Force Fields
	public List<Cloud> clouds;
	public List<Beam> beams;
	public List<Vine> vines;
	public List<SprayDrop> sprayDrops;
	public List<Portal> portals;
	public List<Furniture> furniture;
	public List<Explosion> explosions;

	Area visibleRememberArea = null;

	List<Environment> subEnvironments;
	Environment parent = null;
	int globalX, globalY;
	public int id;

	// Sounds
	public List<SoundEffect> ongoingSounds = new ArrayList<SoundEffect>();

	public Environment(int globalx, int globaly, int width1, int height1)
	{
		globalX = globalx;
		globalY = globaly;
		width = width1;
		height = height1;
		id = Environment.giveID();
		subEnvironments = new ArrayList<Environment>();
		amountOfElements = Resources.numOfElements;
		wallHealths = new int[width][height];
		wallTypes = new int[width][height];
		poolHealths = new int[width][height];
		poolTypes = new int[width][height];
		poolImages = new BufferedImage[width][height];
		floorTypes = new int[width][height];
		cornerCracks = new int[width][height];
		wCornerStyles = new int[width + 1][height + 1][amountOfElements + 1]; // 12 = cement
		pCornerStyles = new int[width + 1][height + 1][amountOfElements];
		pCornerTransparencies = new int[width + 1][height + 1][amountOfElements];
		checkedSquares = new boolean[width][height];
		widthPixels = width * squareSize;
		heightPixels = height * squareSize;
		// default shadow position is directly below.
		shadowX = 0;
		shadowY = 0;
		visualEffects = new ArrayList<VisualEffect>();
		people = new ArrayList<Person>();
		AFFs = new ArrayList<ArcForceField>();
		balls = new ArrayList<Ball>();
		debris = new ArrayList<Debris>();
		uitexts = new ArrayList<UIText>();
		FFs = new ArrayList<ForceField>();
		clouds = new ArrayList<Cloud>();
		beams = new ArrayList<Beam>();
		vines = new ArrayList<Vine>();
		sprayDrops = new ArrayList<SprayDrop>();
		portals = new ArrayList<Portal>();
		furniture = new ArrayList<Furniture>();
		explosions = new ArrayList<Explosion>();

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
			{
				wallHealths[x][y] = -1;
				wallTypes[x][y] = -1;
				poolHealths[x][y] = -1;
				poolTypes[x][y] = -1;
				poolImages[x][y] = null;
				floorTypes[x][y] = -1;
				cornerCracks[x][y] = -1;
				for (int i = 0; i < wCornerStyles[0][0].length; i++)
					wCornerStyles[x][y][i] = -1;
				for (int i = 0; i < pCornerStyles[0][0].length; i++)
				{
					pCornerStyles[x][y][i] = -1;
					pCornerTransparencies[x][y][i] = 100;
				}
			}
		// random cloud generation
		for (int i = 0; i < numOfClouds; i++)
		{
			int x = MAIN.random.nextInt(widthPixels * 3) - widthPixels;
			int y = MAIN.random.nextInt(heightPixels * 3) - heightPixels;
			int z = minCloudHeight + MAIN.random.nextInt(maxCloudHeight - minCloudHeight);
			int type = MAIN.random.nextInt(Resources.clouds.size());
			clouds.add(new Cloud(x, y, z, type));
		}
		windDirection = new Point(MAIN.random.nextInt(11) - 5, MAIN.random.nextInt(11) - 5);
	}

	private int healthSum = 0, poolNum = 0;
	private boolean[][] checkedSquares;// = new boolean[width][height];

	boolean moveBall(Ball b, double deltaTime)
	{
		// return false if ball was destroyed
		deltaTime *= b.timeEffect;
		Portal intersectedPortal = b.intersectedPortal;
		boolean startedAbovePortal = false;
		if (intersectedPortal != null)
			startedAbovePortal = (intersectedPortal.end.x - intersectedPortal.start.x) * (b.y - intersectedPortal.start.y) > (intersectedPortal.end.y - intersectedPortal.start.y)
					* (b.x - intersectedPortal.start.x);

		double velocityLeft = Math.sqrt(b.xVel * b.xVel + b.yVel * b.yVel) * deltaTime;
		double moveQuantumX = b.xVel / velocityLeft * deltaTime;
		double moveQuantumY = b.yVel / velocityLeft * deltaTime; // vector combination of moveQuantumX and moveQuantumY is equal to 1 pixel per frame.

		// This function moves the physics object one pixel towards their direction, until they can't move anymore or they collide with something.
		while (velocityLeft > 0)
		{
			if (velocityLeft < 1)
			{ // last part of movement
				moveQuantumX *= velocityLeft;
				moveQuantumY *= velocityLeft;
				velocityLeft = 0;
			}
			else
				// non-last parts of movement
				velocityLeft--;
			// Move p a fraction
			// IMPORTANT NOTE!!!!! moveQuantumX and moveQuantumY don't update every check, so if the velocity or the angle change they should be recalculated immediately afterwards (like when a ball
			// bounces).
			b.x += moveQuantumX;
			b.y += moveQuantumY;

			// if ball exits edge of environment
			if (b.x - b.radius < 0 || b.y - b.radius < 0 || b.x + b.radius > heightPixels || b.y + b.radius > heightPixels)
				return false;

			if (b.z < 1)
			{
				// check collisions with walls in the environment, locked to a grid
				for (int i = (int) (b.x - b.radius); velocityLeft > 0 && i / squareSize <= (int) (b.x + b.radius) / squareSize; i += squareSize)
					for (int j = (int) (b.y - b.radius); velocityLeft > 0 && j / squareSize <= (int) (b.y + b.radius) / squareSize; j += squareSize)
					{
						if (wallTypes[i / squareSize][j / squareSize] != -1)
						{
							Point p = new Point((i / squareSize) * squareSize, (j / squareSize) * squareSize);
							double px = b.x, py = b.y;
							// point on rectangle closest to circle. (snaps the point to the rectangle, pretty much, if the circle center is inside the rectangle there isn't snapping, but this is fine
							// since it will detect a collision as a result)

							if (px > p.x + squareSize)
								px = p.x + squareSize;
							if (px < p.x)
								px = p.x;
							if (py > p.y + squareSize)
								py = p.y + squareSize;
							if (py < p.y)
								py = p.y;

							// distance check:
							if (Math.pow(b.x - px, 2) + Math.pow(b.y - py, 2) < Math.pow(b.radius, 2))
							{
								// collision confirmed.
								// Resolving collision:
								boolean bounce = false;
								// TODO balls bouncing off certain walls?
								if (bounce)
								{
									double prevVelocity = velocityLeft;
									collideWithWall(b, i / squareSize, j / squareSize, (int) px, (int) py);
									b.x -= moveQuantumX;
									b.y -= moveQuantumY;
									velocityLeft *= b.velocity() / prevVelocity * deltaTime; // "velocity" decreases as the thing moves. If speed is decreased, velocity is multiplied by
									// the ratio of the previous speed and the current one.
									if (velocityLeft != 0)
									{
										moveQuantumX = b.xVel / velocityLeft * deltaTime;
										moveQuantumY = b.yVel / velocityLeft * deltaTime;
										b.x += moveQuantumX;
										b.y += moveQuantumY;
									}
								}
								else
								{
									damageWall(i / squareSize, j / squareSize, b.getDamage() + b.getPushback(), EP.damageType(b.elementNum));
									// debris
									ballDebris(b, "wall", b.angle());
									// ball was destroyed
									return false;
								}
							}
						}
					}
				// furniture
				for (Furniture f : furniture)
					if (b.z <= f.z + f.height)
						// to avoid needless computation, This line tests basic hitbox collisions first
						if (f.x - 0.5 * f.w <= b.x + b.radius && f.x + 0.5 * f.w >= b.x - b.radius && f.y - 0.5 * f.w <= b.y + b.radius && f.y + 0.5 * f.w >= b.y - b.radius)
						{
						while (f.rotation < 0)
						f.rotation += 2 * Math.PI;
						while (f.rotation >= 2 * Math.PI)
						f.rotation -= 2 * Math.PI;
						Point ballCenter = new Point((int) b.x, (int) b.y);
						// pow2 to avoid using Math.sqrt(), which is supposedly computationally expensive.
						double ballRadiusPow2 = Math.pow(b.radius, 2);
						Point[] fPoints = f.getPoints();
						boolean collision = false;
						if (0 <= Methods.DotProduct(fPoints[0], ballCenter, fPoints[1]) && Methods.DotProduct(fPoints[0], ballCenter, fPoints[1]) <= f.h * f.h && 0 <= Methods.DotProduct(fPoints[0], ballCenter, fPoints[3]) && Methods.DotProduct(fPoints[0], ballCenter, fPoints[3]) <= f.w * f.w)
							// circle center is within furniture
							collision = true;
						else
						{
						if (Methods.SegmentToPointDistancePow2(fPoints[0], fPoints[1], ballCenter) < ballRadiusPow2)
						collision = true;
						else if (Methods.SegmentToPointDistancePow2(fPoints[2], fPoints[3], ballCenter) < ballRadiusPow2)
						collision = true;
						if (Methods.SegmentToPointDistancePow2(fPoints[1], fPoints[2], ballCenter) < ballRadiusPow2)
						collision = true;
						else if (Methods.SegmentToPointDistancePow2(fPoints[3], fPoints[0], ballCenter) < ballRadiusPow2)
						collision = true;
						}
						if (collision)
						{
						damageFurniture(f, b.getDamage() + b.getPushback(), EP.damageType(b.elementNum));
						// debris
						ballDebris(b, "wall", b.angle());
						// ball was destroyed
						return false;
						}
						}
			}

			// check collisions with people!
			peopleLoop: for (Person p : people)
			{
				for (Evasion e : b.evasions)
					if (e.id == p.id)
						continue peopleLoop;
				if (p.highestPoint() > b.z && p.z < b.z + b.height)
					if (!p.ghostMode || EP.damageType(b.elementNum) == 4 || EP.damageType(b.elementNum) == 2) // shock and fire
						// temp collide calculation
						if (Math.sqrt(Math.pow(p.x - b.x, 2) + Math.pow(p.y - b.y, 2)) < p.radius / 2 + b.radius)
						{
							boolean bounce = false;
							// TODO find out which power causes bounce skin
							for (int i = 0; i < p.abilities.size(); i++)
								if (p.abilities.get(i).name.equals("some_bounce_ability"))
									bounce = true;
							if (bounce)
							{
								if (b.x - b.radius < p.x + 0.5 * p.radius || b.x + b.radius > p.x - 0.5 * p.radius)
									b.xVel = -b.xVel;
								if (b.y - b.radius < p.y + 0.5 * p.radius || b.y + b.radius > p.y - 0.5 * p.radius)
									b.yVel = -b.yVel;
							}
							else
							{
								// TODO add checks for ball masses and timeEffects, maybe they don't shatter
								// damage person
								if (checkForEvasion(p))
									b.evadedBy(p);
								else
								{
									hitPerson(p, b.getDamage(), b.getPushback(), b.angle(), b.elementNum);
									if (p instanceof NPC)
										((NPC) p).justCollided = true;
									ballDebris(b, "shatter", b.angle());
									// destroy ball
									return false;
								}
							}
						}
			}

			// check collisions with arc force fields
			for (ArcForceField aff : AFFs)
			{
				if (!(b.creator.equals(aff.target) && aff.type == ArcForceField.Type.MOBILE_BUBBLE)) // balls phase through protective bubbles of their owners
					if (aff.z + aff.height > b.z && aff.z < b.z + b.height)
					{
						double angleToBall = Math.atan2(b.y - aff.y, b.x - aff.x);
						while (angleToBall < 0)
							angleToBall += 2 * Math.PI;
						boolean withinAngles = false;
						if (aff.arc >= TAU)
							withinAngles = true;
						else
						{
							double minAngle = (aff.rotation - (aff.arc + 2 * b.radius / aff.maxRadius) / 2);
							double maxAngle = (aff.rotation + (aff.arc + 2 * b.radius / aff.maxRadius) / 2);
							while (minAngle < 0)
								minAngle += 2 * Math.PI;
							while (minAngle >= 2 * Math.PI)
								minAngle -= 2 * Math.PI;
							while (maxAngle < 0)
								maxAngle += 2 * Math.PI;
							while (maxAngle >= 2 * Math.PI)
								maxAngle -= 2 * Math.PI;
							// Okay so here's a thing: I assume the circle is a point, and increase the aff's dimensions for the calculation, and it's almost precise!
							if (minAngle < maxAngle)
							{
								if (angleToBall > minAngle && angleToBall < maxAngle)
									withinAngles = true;
							}
							else if (angleToBall > minAngle || angleToBall > maxAngle)
								withinAngles = true;
						}
						if (withinAngles)
						{
							double distancePow2 = Math.pow(aff.y - b.y, 2) + Math.pow(aff.x - b.x, 2);
							if (distancePow2 > Math.pow(aff.minRadius - b.radius, 2) && distancePow2 < Math.pow(aff.maxRadius + b.radius, 2))
							// That's totally not a legit collision check, but honestly? it's pretty darn close, according to my intuition.
							{
								if (aff.elementNum == 6 && EP.damageType(b.elementNum) == 4) // electricity and energy balls bounce off of energy
								{
									double damage = (b.getDamage() + b.getPushback()) * 0.5; // half damage, because the ball bounces
									damageArcForceField(aff, damage, new Point((int) (aff.x + aff.maxRadius * Math.cos(angleToBall)), (int) (aff.y + aff.maxRadius * Math.sin(angleToBall))),
											EP.damageType(b.elementNum));
									hitPerson(aff.target, 0, 0.5 * b.getPushback(), b.angle(), b.elementNum); // push, not harm
									// TODO cool sparks
									// PHYSICS
									double angle = 2 * angleToBall - b.angle() + Math.PI;
									// avoiding repeat-bounce immediately afterwards
									moveQuantumX = Math.cos(angle);
									moveQuantumY = Math.sin(angle);
									double velocity = b.velocity();
									b.xVel = velocity * moveQuantumX;
									b.yVel = velocity * moveQuantumY;
									// avoiding it some more
									b.x += moveQuantumX;
									b.y += moveQuantumY;
								}
								else if (distancePow2 > aff.maxRadius * aff.maxRadius
										&& (aff.elementNum == 12 || (EP.damageType(aff.elementNum) > 1 && EP.damageType(aff.elementNum) == EP.damageType(b.elementNum)))) // if bubble, or damage resistance
								{
									// bounce
									double ballAngle = b.angle();
									double lineAngle = angleToBall + TAU / 4;
									while (lineAngle < 0)
										lineAngle += TAU;
									while (lineAngle >= TAU)
										lineAngle -= TAU;
									while (ballAngle < 0)
										ballAngle += TAU;
									while (ballAngle >= TAU)
										ballAngle -= TAU;
									double newBallAngle = 2 * lineAngle - ballAngle; // math
									double velocity = b.velocity();
									b.xVel = velocity * Math.cos(newBallAngle);
									b.yVel = velocity * Math.sin(newBallAngle);
									b.x -= 4 * moveQuantumX; // avoid ball stickiness
									b.y -= 4 * moveQuantumY;
								}
								else
								{
									// TODO damage depends on ball speed maybe?a
									// TODO water strong against fire, electricity unblockable by some and entirely blockable by others, , bouncing from metal, etc.
									double damage = b.getDamage() + b.getPushback();
									damageArcForceField(aff, damage, new Point((int) (aff.x + aff.maxRadius * Math.cos(angleToBall)), (int) (aff.y + aff.maxRadius * Math.sin(angleToBall))),
											EP.damageType(b.elementNum));
									hitPerson(aff.target, 0, 0.5 * b.getPushback(), b.angle(), b.elementNum); // push, nor harm

									// Special effects! debris!
									ballDebris(b, "arc force field", angleToBall);
									return false;
								}
							}
						}
					}
			}

			// Force Fields
			for (ForceField ff : FFs)
			{
				if (ff.z + ff.height > b.z && ff.z < b.z + b.height)
					// to avoid needless computation, This line tests basic hitbox collisions first
					if (ff.x - 0.5 * ff.length <= b.x + b.radius && ff.x + 0.5 * ff.length >= b.x - b.radius && ff.y - 0.5 * ff.length <= b.y + b.radius && ff.y + 0.5 * ff.length >= b.y - b.radius)
					{
					boolean bounce = true;
					// TODO move it to once per frame in the FF's code area in frame()
					while (ff.rotation < 0)
					ff.rotation += 2 * Math.PI;
					while (ff.rotation >= 2 * Math.PI)
					ff.rotation -= 2 * Math.PI;
					Point ballCenter = new Point((int) b.x, (int) b.y);
					// pow2 to avoid using Math.sqrt(), which is supposedly computationally expensive.
					double ballRadiusPow2 = Math.pow(b.radius, 2);
					// TODO also test if circle is entirely within the forcefield's rectangle
					/*
					 * four cases because four vertices, and each has its own visual effect In cases 01 and 23, the bounce angle is -Math.PI. but in cases 12 and 30 it's -0. Because rectangle. I can split them to two if-else-ifs because a circle
					 * can't collide with more than 2 of the vertices at once, obviously
					 */
					if (0 <= Methods.DotProduct(ff.p[0], ballCenter, ff.p[1]) && Methods.DotProduct(ff.p[0], ballCenter, ff.p[1]) <= ff.width * ff.width && 0 <= Methods.DotProduct(ff.p[0], ballCenter, ff.p[3]) && Methods.DotProduct(ff.p[0], ballCenter, ff.p[3]) <= ff.length * ff.length)
					// circle center is within FF. This basically never ever should happen.
					{
					damageForceField(ff, b.getDamage() + b.getPushback(), ballCenter);

					// FX
					for (int i = 0; i < 7; i++)
					debris.add(new Debris(b.x, b.y, b.z, b.angle() + 4 + i * (4) / 6, b.elementNum, 500));
					return false;
					}
					else
					{
					if (Methods.SegmentToPointDistancePow2(ff.p[0], ff.p[1], ballCenter) < ballRadiusPow2)
					{
					// TODO cool sparks
					if (bounce)
					{
					// PHYSICS
					double angle = 2 * ff.rotation - b.angle() + Math.PI; // 2*rotation - angle + 180
					// avoiding repeat-bounce immediately afterwards
					moveQuantumX = Math.cos(angle);
					moveQuantumY = Math.sin(angle);
					double velocity = b.velocity();
					b.xVel = velocity * moveQuantumX;
					b.yVel = velocity * moveQuantumY;
					// avoiding it some more
					b.x += moveQuantumX;
					b.y += moveQuantumY;
					}
					}
					else if (Methods.SegmentToPointDistancePow2(ff.p[2], ff.p[3], ballCenter) < ballRadiusPow2)
					{
					// TODO cool sparks
					if (bounce)
					{
					// PHYSICS
					double angle = 2 * ff.rotation - b.angle() + Math.PI;// 2*rotation - angle + 180
					// avoiding repeat-bounce immediately afterwards
					moveQuantumX = Math.cos(angle);
					moveQuantumY = Math.sin(angle);
					double velocity = b.velocity();
					b.xVel = velocity * moveQuantumX;
					b.yVel = velocity * moveQuantumY;
					// avoiding it some more
					b.x += moveQuantumX;
					b.y += moveQuantumY;
					}
					}
					if (Methods.SegmentToPointDistancePow2(ff.p[1], ff.p[2], ballCenter) < ballRadiusPow2)
					{
					// TODO cool sparks
					if (bounce)
					{
					// PHYSICS
					double angle = 2 * ff.rotation - b.angle();// 2*rotation - angle
					// avoiding repeat-bounce immediately afterwards
					moveQuantumX = Math.cos(angle);
					moveQuantumY = Math.sin(angle);
					double velocity = b.velocity();
					b.xVel = velocity * moveQuantumX;
					b.yVel = velocity * moveQuantumY;
					// avoiding it some more
					b.x += moveQuantumX;
					b.y += moveQuantumY;
					}
					}
					else if (Methods.SegmentToPointDistancePow2(ff.p[3], ff.p[0], ballCenter) < ballRadiusPow2)
					{
					// TODO cool sparks
					if (bounce)
					{
					// PHYSICS
					double angle = 2 * ff.rotation - b.angle(); // 2*rotation - angle
					// avoiding repeat-bounce immediately afterwards
					moveQuantumX = Math.cos(angle);
					moveQuantumY = Math.sin(angle);
					double velocity = b.velocity();
					b.xVel = velocity * moveQuantumX;
					b.yVel = velocity * moveQuantumY;
					// avoiding it some more
					b.x += moveQuantumX;
					b.y += moveQuantumY;
					}
					}
					}
					}
			}
			for (Ball b2 : balls)
			{
				if (b == b2 || b2.mass <= 0 || b2.velocity() <= 0)
					continue;
				// TODO testing for evasion, etc.
				if (b2.z + b2.height > b.z && b2.z < b.z + b.height)
					if (Math.pow(b2.x - b.x, 2) + Math.pow(b2.y - b.y, 2) < Math.pow(b2.radius + b.radius, 2))
					{
						boolean bounce = false;
						if (bounce)
						{
							// TODO balls bounce from each other (shouldn't be hard)
						}
						else
						{
							if (b.mass > b2.mass)
							{
								ballDebris(b2, "shatter", b2.angle());
								// collisions reduce mass from the stronger ball
								b.mass -= b2.mass;
								b2.xVel = 0;
								b2.yVel = 0;
								b2.mass = 0;
							}
							else if (b2.mass > b.mass)
							{
								ballDebris(b, "shatter", b.angle());
								b2.mass -= b.mass;
								return false;
							}
							else // equal masses
							{
								ballDebris(b2, "shatter", b2.angle());
								ballDebris(b, "shatter", b.angle());
								b2.xVel = 0;
								b2.yVel = 0;
								b2.mass = 0;
								return false;
							}
						}
					}
			}
		}

		// check if the ball passed through a Portal
		boolean endedAbovePortal = false;
		if (intersectedPortal != null)
			endedAbovePortal = (intersectedPortal.end.x - intersectedPortal.start.x) * (b.y - intersectedPortal.start.y) > (intersectedPortal.end.y - intersectedPortal.start.y)
					* (b.x - intersectedPortal.start.x);
		if (startedAbovePortal != endedAbovePortal && intersectedPortal.partner != null)
		{
			// Portal teleport!
			double angleChange = intersectedPortal.partner.angle - intersectedPortal.angle;
			double angleRelativeToPortal = Math.atan2(b.y - intersectedPortal.y, b.x - intersectedPortal.x);
			double distanceRelativeToPortal = Math.sqrt(Methods.DistancePow2(intersectedPortal.x, intersectedPortal.y, b.x, b.y));
			b.x = intersectedPortal.partner.x + distanceRelativeToPortal * Math.cos(angleRelativeToPortal + angleChange);
			b.y = intersectedPortal.partner.y + distanceRelativeToPortal * Math.sin(angleRelativeToPortal + angleChange);
			b.z += intersectedPortal.partner.z - intersectedPortal.z;
			b.rotation += angleChange;
			double newAngle = b.angle() + angleChange;
			double velocity = b.velocity();
			b.xVel = velocity * Math.cos(newAngle);
			b.yVel = velocity * Math.sin(newAngle);
			intersectedPortal.playPortalSound();
		}

		// ball gravity
		b.z += b.zVel;
		if (b.z < 0)
		{
			// debris
			ballDebris(b, "shatter", b.angle());
			return false;
		}

		return true;
	}

	boolean moveSprayDrop(SprayDrop sd, double deltaTime)
	{
		deltaTime *= sd.timeEffect;
		Portal intersectedPortal = sd.intersectedPortal;
		boolean startedAbovePortal = false;
		if (intersectedPortal != null)
			startedAbovePortal = (intersectedPortal.end.x - intersectedPortal.start.x) * (sd.y - intersectedPortal.start.y) > (intersectedPortal.end.y - intersectedPortal.start.y)
					* (sd.x - intersectedPortal.start.x);

		sd.x += sd.xVel * deltaTime;
		sd.y += sd.yVel * deltaTime;

		// if drop exits edge of environment
		if (sd.x - sd.radius < 0 || sd.y - sd.radius < 0 || sd.x + sd.radius > heightPixels || sd.y + sd.radius > heightPixels)
			return false;

		if (sd.z < 1)
		{
			// check collisions with walls in the environment, locked to a grid
			for (int i = (int) (sd.x - sd.radius); i / squareSize <= (int) (sd.x + sd.radius) / squareSize; i += squareSize)
				for (int j = (int) (sd.y - sd.radius); j / squareSize <= (int) (sd.y + sd.radius) / squareSize; j += squareSize)
				{
					if (wallTypes[i / squareSize][j / squareSize] != -1)
					{
						Point p = new Point((i / squareSize) * squareSize, (j / squareSize) * squareSize);
						double px = sd.x, py = sd.y;
						// point on rectangle closest to circle. (snaps the point to the rectangle, pretty much, if the circle center is inside the rectangle there isn't snapping, but this is fine
						// since it will detect a collision as a result)

						if (px > p.x + squareSize)
							px = p.x + squareSize;
						if (px < p.x)
							px = p.x;
						if (py > p.y + squareSize)
							py = p.y + squareSize;
						if (py < p.y)
							py = p.y;

						// distance check:
						if (Math.pow(sd.x - px, 2) + Math.pow(sd.y - py, 2) < Math.pow(sd.radius, 2))
						{
							damageWall(i / squareSize, j / squareSize, sd.getDamage() + sd.getPushback(), EP.damageType(sd.elementNum));
							sprayDropDebris(sd);
							return false;
						}
					}
				}
			// furniture
			for (Furniture f : furniture)
				if (sd.z <= f.z + f.height)
					// to avoid needless computation, This line tests basic hitbox collisions first
					if (f.x - 0.5 * f.w <= sd.x + sd.radius && f.x + 0.5 * f.w >= sd.x - sd.radius && f.y - 0.5 * f.w <= sd.y + sd.radius && f.y + 0.5 * f.w >= sd.y - sd.radius)
					{
					while (f.rotation < 0)
					f.rotation += 2 * Math.PI;
					while (f.rotation >= 2 * Math.PI)
					f.rotation -= 2 * Math.PI;
					Point ballCenter = new Point((int) sd.x, (int) sd.y);
					// pow2 to avoid using Math.sqrt(), which is supposedly computationally expensive.
					double ballRadiusPow2 = Math.pow(sd.radius, 2);
					Point[] fPoints = f.getPoints();
					boolean collision = false;
					if (0 <= Methods.DotProduct(fPoints[0], ballCenter, fPoints[1]) && Methods.DotProduct(fPoints[0], ballCenter, fPoints[1]) <= f.h * f.h && 0 <= Methods.DotProduct(fPoints[0], ballCenter, fPoints[3]) && Methods.DotProduct(fPoints[0], ballCenter, fPoints[3]) <= f.w * f.w)
						// circle center is within furniture
						collision = true;
					else
					{
					if (Methods.SegmentToPointDistancePow2(fPoints[0], fPoints[1], ballCenter) < ballRadiusPow2)
					collision = true;
					else if (Methods.SegmentToPointDistancePow2(fPoints[2], fPoints[3], ballCenter) < ballRadiusPow2)
					collision = true;
					if (Methods.SegmentToPointDistancePow2(fPoints[1], fPoints[2], ballCenter) < ballRadiusPow2)
					collision = true;
					else if (Methods.SegmentToPointDistancePow2(fPoints[3], fPoints[0], ballCenter) < ballRadiusPow2)
					collision = true;
					}
					if (collision)
					{
					damageFurniture(f, sd.getDamage() + sd.getPushback(), EP.damageType(sd.elementNum));
					// debris
					sprayDropDebris(sd);
					// ball was destroyed
					return false;
					}
					}
		}

		// check collisions with people!

		peopleLoop: for (Person p : people)
		{
			for (Evasion e : sd.evasions)
				if (e.id == p.id)
					continue peopleLoop;

			if (p.highestPoint() > sd.z && p.z < sd.z + sd.height)
				if (!p.ghostMode || EP.damageType(sd.elementNum) == 4 || EP.damageType(sd.elementNum) == 2) // shock and fire
					// temp collide calculation
					if (Math.sqrt(Math.pow(p.x - sd.x, 2) + Math.pow(p.y - sd.y, 2)) < p.radius / 2 + sd.radius)
					{
						if (checkForEvasion(p))
							sd.evadedBy(p);
						else
						{
							// damage person
							hitPerson(p, sd.getDamage(), sd.getPushback(), sd.angle(), sd.elementNum);
							sprayDropDebris(sd);
							// destroy this
							return false;
						}
					}
		}

		// check collisions with arc force fields
		for (ArcForceField aff : AFFs)
		{
			if (!(sd.creator.equals(aff.target) && aff.type == ArcForceField.Type.MOBILE_BUBBLE)) // phase through protective bubbles of owners
				if (aff.z + aff.height > sd.z && aff.z < sd.z + sd.height)
				{
					double distancePow2 = Math.pow(aff.y - sd.y, 2) + Math.pow(aff.x - sd.x, 2);
					if (distancePow2 > Math.pow(aff.minRadius - sd.radius, 2) && distancePow2 < Math.pow(aff.maxRadius + sd.radius, 2))
					{
						double angleToDrop = Math.atan2(sd.y - aff.y, sd.x - aff.x);
						while (angleToDrop < 0)
							angleToDrop += 2 * Math.PI;
						boolean withinAngles = false;
						if (aff.arc >= TAU)
							withinAngles = true;
						else
						{
							double minAngle = (aff.rotation - (aff.arc + 2 * sd.radius / aff.maxRadius) / 2);
							double maxAngle = (aff.rotation + (aff.arc + 2 * sd.radius / aff.maxRadius) / 2);
							while (minAngle < 0)
								minAngle += 2 * Math.PI;
							while (minAngle >= 2 * Math.PI)
								minAngle -= 2 * Math.PI;
							while (maxAngle < 0)
								maxAngle += 2 * Math.PI;
							while (maxAngle >= 2 * Math.PI)
								maxAngle -= 2 * Math.PI;
							// Okay so here's a thing: I assume the circle is a point, and increase the aff's dimensions for the calculation, and it's almost precise!
							if (minAngle < maxAngle)
							{
								if (angleToDrop > minAngle && angleToDrop < maxAngle)
									withinAngles = true;
							}
							else if (angleToDrop > minAngle || angleToDrop > maxAngle)
								withinAngles = true;
						}
						if (withinAngles)
						{
							damageArcForceField(aff, sd.getDamage(), new Point((int) (aff.x + aff.maxRadius * Math.cos(angleToDrop)), (int) (aff.y + aff.maxRadius * Math.sin(angleToDrop))),
									EP.damageType(sd.elementNum));
							hitPerson(aff.target, 0, 0.5 * sd.getPushback(), sd.angle(), sd.elementNum);

							sprayDropDebris(sd);
							return false;
						}
					}
				}
		}

		// Force Fields
		for (ForceField ff : FFs)
			if (ff.z + ff.height > sd.z && ff.z < sd.z + sd.height)
				// to avoid needless computation, This line tests basic hitbox collisions first
				if (ff.x - 0.5 * ff.length <= sd.x + sd.radius && ff.x + 0.5 * ff.length >= sd.x - sd.radius && ff.y - 0.5 * ff.length <= sd.y + sd.radius
						&& ff.y + 0.5 * ff.length >= sd.y - sd.radius)
				{
				// TODO move it to once per frame in the FF's code area in frame()
				while (ff.rotation < 0)
				ff.rotation += 2 * Math.PI;
				while (ff.rotation >= 2 * Math.PI)
				ff.rotation -= 2 * Math.PI;
				Point dropCenter = new Point((int) sd.x, (int) sd.y);
				// pow2 to avoid using Math.sqrt(), which is supposedly computationally expensive.
				double ballRadiusPow2 = Math.pow(sd.radius, 2);
				// TODO instead, test if center is within the forcefield's rectangle!!!!!!!!!!!!!!!!!!!!!!!!!
				if (0 <= Methods.DotProduct(ff.p[0], dropCenter, ff.p[1]) && Methods.DotProduct(ff.p[0], dropCenter, ff.p[1]) <= ff.width * ff.width && 0 <= Methods.DotProduct(ff.p[0], dropCenter, ff.p[3]) && Methods.DotProduct(ff.p[0], dropCenter, ff.p[3]) <= ff.length * ff.length)
				// circle center is within FF?
				{
				damageForceField(ff, sd.getDamage() + sd.getPushback(), dropCenter);

				sprayDropDebris(sd);
				return false;
				}
				else
				{
				boolean yes = false;
				if (Methods.SegmentToPointDistancePow2(ff.p[0], ff.p[1], dropCenter) < ballRadiusPow2)
				yes = true;
				else if (Methods.SegmentToPointDistancePow2(ff.p[2], ff.p[3], dropCenter) < ballRadiusPow2)
				yes = true;
				if (Methods.SegmentToPointDistancePow2(ff.p[1], ff.p[2], dropCenter) < ballRadiusPow2)
				yes = true;
				else if (Methods.SegmentToPointDistancePow2(ff.p[3], ff.p[0], dropCenter) < ballRadiusPow2)
				yes = true;
				if (yes)
				{
				damageForceField(ff, sd.getDamage(), dropCenter);
				sprayDropDebris(sd);
				return false;
				}
				}
				}

		// check if the spray drop passed through a Portal
		boolean endedAbovePortal = false;
		if (intersectedPortal != null)
			endedAbovePortal = (intersectedPortal.end.x - intersectedPortal.start.x) * (sd.y - intersectedPortal.start.y) > (intersectedPortal.end.y - intersectedPortal.start.y)
					* (sd.x - intersectedPortal.start.x);
		if (startedAbovePortal != endedAbovePortal && intersectedPortal.partner != null)
		{
			// Portal teleport!
			double angleChange = intersectedPortal.partner.angle - intersectedPortal.angle;
			double angleRelativeToPortal = Math.atan2(sd.y - intersectedPortal.y, sd.x - intersectedPortal.x);
			double distanceRelativeToPortal = Math.sqrt(Methods.DistancePow2(intersectedPortal.x, intersectedPortal.y, sd.x, sd.y));
			sd.x = intersectedPortal.partner.x + distanceRelativeToPortal * Math.cos(angleRelativeToPortal + angleChange);
			sd.y = intersectedPortal.partner.y + distanceRelativeToPortal * Math.sin(angleRelativeToPortal + angleChange);
			sd.z += intersectedPortal.partner.z - intersectedPortal.z;
			sd.rotation += angleChange;
			double newAngle = sd.angle() + angleChange;
			double velocity = sd.velocity();
			sd.xVel = velocity * Math.cos(newAngle);
			sd.yVel = velocity * Math.sin(newAngle);
		}

		// gravity
		sd.z += sd.zVel;
		if (sd.z < 0)
		{
			sprayDropDebris(sd);
			return false;
		}

		return true;

	}

	void moveDebris(Debris d, double deltaTime)
	{
		Portal intersectedPortal = d.intersectedPortal;
		boolean startedAbovePortal = false;
		if (intersectedPortal != null)
			startedAbovePortal = (intersectedPortal.end.x - intersectedPortal.start.x) * (d.y - intersectedPortal.start.y) > (intersectedPortal.end.y - intersectedPortal.start.y)
					* (d.x - intersectedPortal.start.x);

		d.x += d.velocity * Math.cos(d.angle) * deltaTime;
		d.y += d.velocity * Math.sin(d.angle) * deltaTime;
		// check if the debris passed through a Portal
		boolean endedAbovePortal = false;
		if (intersectedPortal != null)
			endedAbovePortal = (intersectedPortal.end.x - intersectedPortal.start.x) * (d.y - intersectedPortal.start.y) > (intersectedPortal.end.y - intersectedPortal.start.y)
					* (d.x - intersectedPortal.start.x);
		if (startedAbovePortal != endedAbovePortal && intersectedPortal.partner != null)
		{
			// Portal teleport!
			double angleChange = intersectedPortal.partner.angle - intersectedPortal.angle;
			double angleRelativeToPortal = Math.atan2(d.y - intersectedPortal.y, d.x - intersectedPortal.x);
			double distanceRelativeToPortal = Math.sqrt(Methods.DistancePow2(intersectedPortal.x, intersectedPortal.y, d.x, d.y));
			d.x = intersectedPortal.partner.x + distanceRelativeToPortal * Math.cos(angleRelativeToPortal + angleChange);
			d.y = intersectedPortal.partner.y + distanceRelativeToPortal * Math.sin(angleRelativeToPortal + angleChange);
			d.z += intersectedPortal.partner.z - intersectedPortal.z;
			d.rotation += angleChange;
			d.angle = d.angle + angleChange;
		}
	}

	void movePerson(Person p, double deltaTime)
	{
		deltaTime *= p.timeEffect;
		Portal intersectedPortal = p.intersectedPortal;
		boolean startedAbovePortal = false;
		if (intersectedPortal != null)
			startedAbovePortal = (intersectedPortal.end.x - intersectedPortal.start.x) * (p.y - intersectedPortal.start.y) > (intersectedPortal.end.y - intersectedPortal.start.y)
					* (p.x - intersectedPortal.start.x);
		double velocityLeft = Math.sqrt(p.xVel * p.xVel + p.yVel * p.yVel) * deltaTime;

		p.lastSpeed = velocityLeft / deltaTime;
		double moveQuantumX = p.xVel / velocityLeft * deltaTime;
		double moveQuantumY = p.yVel / velocityLeft * deltaTime; // vector combination of moveQuantumX and moveQuantumY is equal to 1 pixel per frame.
		// This function moves the physics object one pixel towards their direction, until they can't move anymore or they collide with something.
		if (velocityLeft > 0)
			p.insideWall = false; // pretty important
		else if (velocityLeft == 0)
		{
			moveQuantumX = 0; // was NaN
			moveQuantumY = 0; // was NaN
		}
		Rectangle2D personRect = new Rectangle2D.Double((int) p.x - p.radius, (int) p.y - p.radius, p.radius * 2, p.radius * 2); // for FF collisions

		Charge charge = null;
		Elastic elastic = null;
		for (Ability a : p.abilities)
			if (a.on)
			{
				if (a instanceof Charge)
					charge = (Charge) a;
				if (a instanceof Elastic)
					elastic = (Elastic) a;
			}

		while (velocityLeft > 0)
		{
			if (velocityLeft < 1)
			{ // last part of movement
				moveQuantumX *= velocityLeft;
				moveQuantumY *= velocityLeft;
			}
			else
				// non-last parts of movement
				velocityLeft -= 1;
			// Move p a fraction
			p.x += moveQuantumX;
			p.y += moveQuantumY;

			// Furniture
			if (p.z < 1)
				if (!p.ghostMode) // ghosts pass through stuff
					for (Furniture f : furniture)
					{
						// Copy of the FF collision code
						boolean collided = false;
						boolean collidedWithACorner = false;
						double newAngle = 0;
						Point[] fPoints = f.getPoints();
						for (Point p1 : fPoints)
							if (p1.x > p.x - p.radius && p1.x < p.x + p.radius && p1.y > p.y - p.radius && p1.y < p.y + p.radius)
							{
								collidedWithACorner = true;
								collided = true;
								newAngle = Math.atan2(p.y - f.y, p.x - f.x);
							}
						if (!collidedWithACorner)
						{
							double personAngle = p.angle();
							Line2D l1 = new Line2D.Double(fPoints[0].x, fPoints[0].y, fPoints[3].x, fPoints[3].y);
							Line2D l2 = new Line2D.Double(fPoints[0].x, fPoints[0].y, fPoints[1].x, fPoints[1].y);
							Line2D l3 = new Line2D.Double(fPoints[2].x, fPoints[2].y, fPoints[1].x, fPoints[1].y);
							Line2D l4 = new Line2D.Double(fPoints[2].x, fPoints[2].y, fPoints[3].x, fPoints[3].y);
							if (personRect.intersectsLine(l1))
							{
								collided = true;
								newAngle = 2 * f.rotation - personAngle;
							}
							if (personRect.intersectsLine(l2))
							{
								collided = true;
								newAngle = 2 * f.rotation - personAngle + Math.PI;
							}
							if (personRect.intersectsLine(l3))
							{
								collided = true;
								newAngle = 2 * f.rotation - personAngle;
							}
							if (personRect.intersectsLine(l4))
							{
								collided = true;
								newAngle = 2 * f.rotation - personAngle + Math.PI;
							}

						}
						if (collided)
						{
							if (charge != null && p.velocityPow2() >= charge.minimumVelocityPow2)
								damageFurniture(f, (charge.damage + charge.pushback) * 4, 0); // blunt damage, x4 because charge deals more to inorganics
							if (f.life > 0)
							{
								if (f.type == Furniture.Type.DOOR)
								{
									if (f.state == 0) // closed
									{
										f.activate();
										continue;
									}
									if (f.state == 1) // open
										continue;
									if (f.state == 2) // locked
										;
								}
								// THIS IS NOT GOOD CODE, THIS IS BAD CODE, BUT I CAN'T MAKE THIS PHYSICS THING WORK LIKE I WANT IT TO
								p.x -= moveQuantumX;
								p.y -= moveQuantumY;
								// attempt at physics
								double bounceEfficiency = 0.6;
								if (elastic != null && p.velocityPow2() >= elastic.minimumVelocityPow2)
									bounceEfficiency = 1;
								double velocity = bounceEfficiency * p.velocity();
								p.xVel = velocity * Math.cos(newAngle);
								p.yVel = velocity * Math.sin(newAngle);
								moveQuantumX = Math.cos(newAngle) * deltaTime;
								moveQuantumY = Math.sin(newAngle) * deltaTime;
								p.x += 80 * moveQuantumX;
								p.y += 80 * moveQuantumY;
								if (p instanceof NPC)
									((NPC) p).justCollided = true;
							}
							else
							// reduce speed by a bit
							{
								p.xVel *= 0.95;
								p.yVel *= 0.95;
							}
						}
					}

			// People-people collisions
			for (Person p2 : people)
				if (!p.equals(p2))
					if (Methods.DistancePow2(p.Point(), p2.Point()) <= Math.pow(p.radius + p2.radius, 2)) // collision check
					{
						if (p2.highestPoint() > p.z && p2.z < p.highestPoint())
						{
							if (!p2.prone)

								if (!p2.dead) // No colliding with dead people! //TODO make it slow you down a bit maybe
								{
									// physics. Assumes the two people are circles.
									// The following code is translated from a StackExchange answer.
									double xVelocity = p2.xVel - p.xVel;
									double yVelocity = p2.yVel - p.yVel;
									double dotProduct = (p2.x - p.x) * xVelocity + (p2.y - p.y) * yVelocity;
									// Neat vector maths, used for checking if the objects moves towards one another.
									if (dotProduct < 0)
									{
										double collisionScale = dotProduct / Methods.DistancePow2(p.x, p.y, p2.x, p2.y);
										double xCollision = (p2.x - p.x) * collisionScale;
										double yCollision = (p2.y - p.y) * collisionScale;
										// The Collision vector is the speed difference projected on the Dist vector,
										// thus it is the component of the speed difference needed for the collision.
										double combinedMass = p.mass + p2.mass;
										double collisionWeightA = 2 * p2.mass / combinedMass;
										double collisionWeightB = 2 * p.mass / combinedMass;
										if (charge != null && p.velocityPow2() >= charge.minimumVelocityPow2)
										{
											if (p.STRENGTH >= p2.STRENGTH)
											{
												collisionWeightA *= 0.20; // 20%
												hitPerson(p2, charge.damage, charge.pushback, p.angle() + TAU / 4 - (int) (Math.random() * 2) * TAU / 2, -1); // angle is 90 degrees to either side of p's angle
												p2.slip(true);
											}
										}
										if (elastic != null && p.velocityPow2() >= elastic.minimumVelocityPow2)
										{
											if (p.STRENGTH >= p2.STRENGTH)
											{
												collisionWeightA = 1;
												hitPerson(p2, elastic.damage, elastic.pushback, p.angle(), -1); // angle is p's movement angle
												p2.slip(true);
											}
										}
										p.xVel += collisionWeightA * xCollision;
										p.yVel += collisionWeightA * yCollision;
										p2.xVel -= collisionWeightB * xCollision;
										p2.yVel -= collisionWeightB * yCollision;
										p.x -= 2 * moveQuantumX; // good enough for most purposes right now
										p.y -= 2 * moveQuantumY;//
										if (p instanceof NPC)
											((NPC) p).justCollided = true;
										if (p2 instanceof NPC)
											((NPC) p2).justCollided = true;
									}
								}
						}
					}
			for (ForceField ff : FFs)
			{
				// checks if (rotated) force field corners are inside person hitbox
				if (ff.z + ff.height > p.z && ff.z < p.highestPoint())
				{
					boolean collidedWithACorner = false;
					for (Point p1 : ff.p)
						if (p1.x > p.x - p.radius && p1.x < p.x + p.radius && p1.y > p.y - p.radius && p1.y < p.y + p.radius)
						{
							collidedWithACorner = true;
							// hitting corners just reverses the person's movement
							p.x -= moveQuantumX;
							p.y -= moveQuantumY;
							p.xVel = -p.xVel;
							p.yVel = -p.yVel;
							p.x += deltaTime * p.xVel;
							p.y += deltaTime * p.yVel;
							hitPerson(p, 5 * deltaTime, 0, 0, 6);// energy
							if (p instanceof NPC)
								((NPC) p).justCollided = true;
						}
					if (!collidedWithACorner)
					{
						Line2D l1 = new Line2D.Double(ff.p[0].x, ff.p[0].y, ff.p[3].x, ff.p[3].y);
						Line2D l2 = new Line2D.Double(ff.p[0].x, ff.p[0].y, ff.p[1].x, ff.p[1].y);
						Line2D l3 = new Line2D.Double(ff.p[2].x, ff.p[2].y, ff.p[1].x, ff.p[1].y);
						Line2D l4 = new Line2D.Double(ff.p[2].x, ff.p[2].y, ff.p[3].x, ff.p[3].y);
						boolean collided = false;
						double lineAngle = 0;
						if (personRect.intersectsLine(l1))
						{
							collided = true;
							lineAngle = ff.rotation + 0.5 * Math.PI;
						}
						if (personRect.intersectsLine(l2))
						{
							collided = true;
							lineAngle = ff.rotation;
						}
						if (personRect.intersectsLine(l3))
						{
							collided = true;
							lineAngle = ff.rotation + 0.5 * Math.PI;
						}
						if (personRect.intersectsLine(l4))
						{
							collided = true;
							lineAngle = ff.rotation;
						}
						if (collided)
						{
							// BUGGY
							// SRSLY
							// TODO
							p.x -= moveQuantumX;
							p.y -= moveQuantumY;
							// attempt at physics
							double personAngle = Math.atan2(moveQuantumY, moveQuantumX); // can also use yVel, xVel
							personAngle = 2 * lineAngle - personAngle + Math.PI;
							double velocity = Math.sqrt(p.xVel * p.xVel + p.yVel * p.yVel);
							p.xVel = velocity * Math.cos(personAngle);
							p.yVel = velocity * Math.sin(personAngle);
							moveQuantumX = Math.cos(personAngle) * deltaTime;
							moveQuantumY = Math.sin(personAngle) * deltaTime;
							p.x += 80 * moveQuantumX;
							p.y += 80 * moveQuantumY;
							// zap
							hitPerson(p, 5 * deltaTime, 0, 0, 6); // energy
							if (p instanceof NPC)
								((NPC) p).justCollided = true;
						}
					}
				}

			}

			// check collisions with walls in the environment, locked to a grid
			List<Point> intersectingWalls = new ArrayList<Point>();
			int minGridX = Math.min(Math.max((int) (p.x - p.radius) / squareSize, 0), width - 1);
			int minGridY = Math.min(Math.max((int) (p.y - p.radius) / squareSize, 0), height - 1);
			int maxGridX = Math.min(Math.max((int) (p.x + p.radius) / squareSize, 0), width - 1);
			int maxGridY = Math.min(Math.max((int) (p.y + p.radius) / squareSize, 0), height - 1);
			for (int i = minGridX; i <= maxGridX; i++)
				for (int j = minGridY; j <= maxGridY; j++)
					if (p.z <= 1 && wallTypes[i][j] != -1)
						intersectingWalls.add(new Point(i, j));
			if (!intersectingWalls.isEmpty())
				if (!p.ghostMode) // ghosts pass through stuff
				{
					if (p.z > 0.1 && p.z <= 1 && p.zVel < 0) // if falling into a wall
					{
						p.z = 1; // standing on a wall
						p.zVel = 0;
						if (p instanceof NPC)
							((NPC) p).justCollided = true;
					}
					else if (p.z < 1)
					{
						Point closestWall = null;
						double closestWallDistancePow2 = Double.MAX_VALUE;
						for (int i = 0; i < intersectingWalls.size(); i++)
						{
							double distancePow2 = Methods.DistancePow2(p.x, p.y, intersectingWalls.get(i).x * squareSize + squareSize / 2, intersectingWalls.get(i).y * squareSize + squareSize / 2);
							if (distancePow2 < closestWallDistancePow2)
							{
								closestWallDistancePow2 = distancePow2;
								closestWall = intersectingWalls.get(i);
							}
						}
						int x = closestWall.x;
						int y = closestWall.y;
						double prevVelocity = velocityLeft;
						if (collideWithWall(p, x, y))
						{
							p.x -= moveQuantumX;
							p.y -= moveQuantumY;
							if (p instanceof NPC)
								((NPC) p).justCollided = true;
							if (p.z > 0 && p.zVel < 0)
							{
								p.z -= p.zVel * deltaTime;
								p.zVel = 0;
							}
							velocityLeft *= Math.sqrt(p.xVel * p.xVel + p.yVel * p.yVel) * deltaTime / prevVelocity;
							// "velocity" decreases as the thing moves. If speed is decreased, velocity is multiplied by the ratio of the previous speed and the current one.
							if (velocityLeft != 0)
							{
								moveQuantumX = p.xVel / velocityLeft * deltaTime;
								moveQuantumY = p.yVel / velocityLeft * deltaTime;
								p.x += moveQuantumX;
								p.y += moveQuantumY;
							}
						}
					}
				}
				else // to avoid ghosts reappearing inside walls
					p.insideWall = true;

			// Portals
			for (Portal por : portals)
				if (por.partner != null && por.highestPoint() > p.z && p.highestPoint() > por.z)
				{
					if (Methods.DistancePow2(por.start, p.Point()) < p.radius * p.radius)
					{
						p.x -= moveQuantumX;
						p.y -= moveQuantumY;
						double angle = Math.atan2(p.y - por.start.y, p.x - por.start.x);
						p.xVel += 11.32 * Math.cos(angle);
						p.yVel += 11.32 * Math.sin(angle);
					}
					if (Methods.DistancePow2(por.end, p.Point()) < p.radius * p.radius)
					{
						p.x -= moveQuantumX;
						p.y -= moveQuantumY;
						double angle = Math.atan2(p.y - por.end.y, p.x - por.end.x);
						p.xVel += 11.32 * Math.cos(angle);
						p.yVel += 11.32 * Math.sin(angle);
					}
				}

			if (velocityLeft < 1) // continue
				velocityLeft = 0;
			personRect = new Rectangle2D.Double((int) p.x - p.radius, (int) p.y - p.radius, p.radius * 2, p.radius * 2);
		}

		// boundary check
		if (p.x < 0)
			p.x = 0;
		if (p.y < 0)
			p.y = 0;
		if (p.x > this.widthPixels - 1)
			p.x = this.widthPixels - 1;
		if (p.y > this.heightPixels - 1)
			p.y = this.heightPixels - 1;

		// check if person passed through a Portal
		/*
		 * NOTE: This WILL fail if the player tries a lot of time in different rottions, "edging" the portal, and so sometimes players will exit on the other side of the portal. Right now this is a known bug, because I'm not really sure how to fix
		 * it, but it shouldn't happen with non-player people or objects. I hope.
		 */
		boolean endedAbovePortal = false;
		if (intersectedPortal != null)
			endedAbovePortal = (intersectedPortal.end.x - intersectedPortal.start.x) * (p.y - intersectedPortal.start.y) > (intersectedPortal.end.y - intersectedPortal.start.y)
					* (p.x - intersectedPortal.start.x);
		if (startedAbovePortal != endedAbovePortal && intersectedPortal.partner != null)
			if (p.timeUntilPortalConfusionIsOver <= 0)
			{
				// Portal teleport!
				double angleChange = intersectedPortal.partner.angle - intersectedPortal.angle;
				double angleRelativeToPortal = Math.atan2(p.y - intersectedPortal.y, p.x - intersectedPortal.x);
				double distanceRelativeToPortal = Math.sqrt(Methods.DistancePow2(intersectedPortal.x, intersectedPortal.y, p.x, p.y));
				if (this.id != intersectedPortal.partner.envID)
				{
					p.portalToOtherEnvironment = intersectedPortal.partner.envID;
					p.portalVariableX = intersectedPortal.partner.x + distanceRelativeToPortal * Math.cos(angleRelativeToPortal + angleChange);
					p.portalVariableY = intersectedPortal.partner.y + distanceRelativeToPortal * Math.sin(angleRelativeToPortal + angleChange);
				}
				else
				{
					p.x = intersectedPortal.partner.x + distanceRelativeToPortal * Math.cos(angleRelativeToPortal + angleChange);
					p.y = intersectedPortal.partner.y + distanceRelativeToPortal * Math.sin(angleRelativeToPortal + angleChange);
					if (p instanceof Player)
					{
						p.portalVariableX = intersectedPortal.x + distanceRelativeToPortal * Math.cos(angleRelativeToPortal + angleChange);
						p.portalVariableY = intersectedPortal.y + distanceRelativeToPortal * Math.sin(angleRelativeToPortal + angleChange);
					}
				}
				p.z += intersectedPortal.partner.z - intersectedPortal.z;
				p.rotation += angleChange;
				double newAngle = p.angle() + angleChange;
				double velocity = p.velocity();
				p.xVel = velocity * Math.cos(newAngle);
				p.yVel = velocity * Math.sin(newAngle);
				intersectedPortal.playPortalSound();
				p.timeUntilPortalConfusionIsOver = 0.1; // For a period of time after portaling, you can't move through more portals.
				if (p instanceof Player)
				{
					((Player) p).portalMovementRotation += angleChange; // player's keys will keep pushing character relative to previous rotation
					((Player) p).portalCameraRotation += angleChange; // player's camera will rotate
				}
				if (p instanceof NPC)
					((NPC) p).path = ((NPC) p).pathFind(new Point((int) (p.x + 1 * Math.cos(newAngle)), (int) (p.y + 1 * Math.sin(newAngle))));
			}
			else
			{
				// Tried to move through portal too soon after previous one
				p.x -= moveQuantumX;
				p.y -= moveQuantumY;
			}

		// extra check for insideWall, in case you stand still
		if (p.ghostMode && p.z < 1)
			for (int i = (int) (p.x - 0.5 * p.radius); i / squareSize <= (int) (p.x + 0.5 * p.radius) / squareSize; i += squareSize)
				for (int j = (int) (p.y - 0.5 * p.radius); j / squareSize <= (int) (p.y + 0.5 * p.radius) / squareSize; j += squareSize)
					if (i / squareSize >= 0 && j / squareSize >= 0 && i / squareSize <= width && j / squareSize <= height)
						if (wallTypes[i / squareSize][j / squareSize] != -1)
							p.insideWall = true;

		// rotate if corpse
		double someConstant = 0.1623542545; // whatever
		if (p.dead)
			if (moveQuantumY != 0 || moveQuantumX != 0)
				p.rotate(Math.atan2(moveQuantumY, moveQuantumX), someConstant * deltaTime);
	}

	public boolean personAFFCollision(Person p, ArcForceField aff)
	{
		if (aff.target.equals(p) && aff.type != ArcForceField.Type.IMMOBILE_BUBBLE)
			return false;
		// checks square first
		Rectangle2D affBox = new Rectangle2D.Double(aff.x - aff.maxRadius, aff.y - aff.maxRadius, aff.maxRadius * 2, aff.maxRadius * 2);
		Rectangle2D personRect = new Rectangle2D.Double((int) p.x - p.radius / 2, (int) p.y - p.radius / 2, p.radius, p.radius);
		if (personRect.intersects(affBox))
		{
			if (aff.z + aff.height > p.z && aff.z < p.highestPoint())
				if (aff.arc < TAU) // if not bubble
				{
					// following code is copied from ball-aff collision
					double angleToPerson = Math.atan2(p.y - aff.y, p.x - aff.x);
					while (angleToPerson < 0)
						angleToPerson += 2 * Math.PI;
					double minAngle = aff.rotation - aff.arc / 2 - Math.tan(p.radius / aff.maxRadius);
					double maxAngle = aff.rotation + aff.arc / 2 + Math.tan(p.radius / aff.maxRadius);
					while (minAngle < 0)
						minAngle += 2 * Math.PI;
					while (minAngle >= 2 * Math.PI)
						minAngle -= 2 * Math.PI;
					while (maxAngle < 0)
						maxAngle += 2 * Math.PI;
					while (maxAngle >= 2 * Math.PI)
						maxAngle -= 2 * Math.PI;
					boolean withinAngles = false;
					if (minAngle < maxAngle)
					{
						if (angleToPerson > minAngle && angleToPerson < maxAngle)
							withinAngles = true;
					}
					else if (angleToPerson > minAngle || angleToPerson < maxAngle)
						withinAngles = true;
					if (withinAngles)
					{
						double distance = Math.sqrt(Math.pow(aff.y - p.y, 2) + Math.pow(aff.x - p.x, 2));
						if (distance > aff.minRadius - p.radius && distance < aff.maxRadius + p.radius)
							return true;
						return false;
					}
				}
				else // much easier
				{
					double distancePow2 = Methods.DistancePow2(aff.x, aff.y, p.x, p.y);
					if (distancePow2 < Math.pow(p.radius + aff.maxRadius, 2))
						return true;
					return false;
				}
		}
		return false;
	}

	boolean collideWithWall(Person p, int x, int y) // x and y in grid
	{
		int wallElement = getWallElement(wallTypes[x][y]);
		// returns whether or not this collision changes the person's speed
		double bounceEfficiency = 0.4; // should depend on element? TODO ?
		Rectangle intersectRect = new Rectangle(x * squareSize, y * squareSize, squareSize, squareSize)
				.intersection(new Rectangle((int) (p.x - 0.5 * p.radius), (int) (p.y - 0.5 * p.radius), (int) (p.radius), (int) (p.radius)));

		double damageToWall = p.mass * p.velocity() * 0.000005;
		double damageToPerson = wallHealths[x][y] * p.velocity() * 0.00004;

		Charge charge = null;
		Elastic elastic = null;
		for (Ability a : p.abilities)
			if (a.on)
			{
				if (a instanceof Charge)
					charge = (Charge) a;
				if (a instanceof Elastic)
					elastic = (Elastic) a;
			}
		if (charge != null && p.velocityPow2() >= charge.minimumVelocityPow2)
		{
			// damage is Charge's damage and pushback, multiplied by how much direct the hit is.
			double angleToWall = Math.atan2(intersectRect.getCenterY() - p.y, intersectRect.getCenterX() - p.x);
			double personAngle = p.angle();
			double directness = Math.abs(Math.cos(angleToWall)) * Math.abs(Math.cos(personAngle)) + Math.abs(Math.sin(angleToWall)) * Math.abs(Math.sin(personAngle));
			damageToWall += (charge.damage + charge.pushback) * directness;
			damageToWall *= 4; // Charge inherently deals more damage to walls
		}
		if (elastic != null && p.velocityPow2() >= elastic.minimumVelocityPow2)
		{
			damageToPerson = 0;
			bounceEfficiency = 1;
		}
		if (wallElement != -2)
			damageWall(x, y, damageToWall, 0); // blunt damage
		if (wallHealths[x][y] > 0) // if wall survives the collision, it deflects the person
		{
			Rectangle2D personRect = new Rectangle2D.Double(p.x - p.radius, p.y - p.radius, p.radius * 2, p.radius * 2);
			double wy = (personRect.getWidth() + intersectRect.getWidth()) * (personRect.getCenterY() - intersectRect.getCenterY());
			double hx = (personRect.getHeight() + intersectRect.getHeight()) * (personRect.getCenterX() - intersectRect.getCenterX());

			double friction = 0.5; // how much speed is wasted on friction (for the dimension that isn't directly colliding with the wall)

			if (wy > hx)
				if (wy > -hx)
				/* top */
				{
					p.yVel = Math.abs(p.yVel) * bounceEfficiency;
					p.xVel -= p.xVel * friction;
				}
				else
				/* left */
				{
					p.xVel = -Math.abs(p.xVel) * bounceEfficiency;
					p.yVel -= p.yVel * friction;
				}
			else if (wy > -hx)
			/* right */
			{
				p.xVel = Math.abs(p.xVel) * bounceEfficiency;
				p.yVel -= p.yVel * friction;
			}
			else
			/* bottom */
			{
				p.yVel = -Math.abs(p.yVel) * bounceEfficiency;
				p.xVel -= p.xVel * friction;
			}

			hitPerson(p, damageToPerson, 0, p.angle() + TAU / 2, wallElement); // damage depends on wall element

			return true;
		}
		else
		{
			// extra debris
			for (int i = 0; i < 5; i++)
				debris.add(new Debris(x * squareSize + 0.5 * squareSize, y * squareSize + 0.5 * squareSize, 0, Math.PI * 2 / 5 * i, wallElement, 200));
			// slow down the person
			double energyLost = 10000;

			if (charge != null && p.velocityPow2() >= charge.minimumVelocityPow2)
			{
				damageToPerson *= 0.20; // 20%
				energyLost *= 0.20; // 20%
			}

			double newVelocity = p.velocity() - energyLost / Math.sqrt(p.mass);
			double ratio = newVelocity / p.velocity();
			p.xVel *= ratio;
			p.yVel *= ratio;

			hitPerson(p, damageToPerson, 0, p.angle() + TAU / 2, -1); // blunt damage

			return false;
		}

	}

	void collideWithWall(RndPhysObj o, int x, int y, int px, int py) // x and y in grid
	{
		final double bounceEfficiency = 1;
		// assumes walls have no angle, of course (they don't)

		if (px == x * squareSize && (o.angle() < Math.PI / 2 || o.angle() >= Math.PI * 3 / 2)) // left side, moving right
			o.xVel = -o.xVel;
		if (px == (x + 1) * squareSize && o.angle() >= Math.PI / 2 && o.angle() < Math.PI * 3 / 2) // right side, moving left
			o.xVel = -o.xVel;
		if (py == y * squareSize && o.angle() < Math.PI) // up side, moving down
			o.yVel = -o.yVel;
		if (py == (y + 1) * squareSize && o.angle() >= Math.PI) // down side, moving up
			o.yVel = -o.yVel;

		o.xVel *= bounceEfficiency;
		o.yVel *= bounceEfficiency;
	}

	public void moveVine(Vine v, double deltaTime)
	{
		double originalAngle = v.rotation;
		v.creator.rotation = v.rotation;
		double desiredAngle = Math.atan2(v.creator.target.y - v.creator.y, v.creator.target.x - v.creator.x);

		if (v.state == 1) // grabbling
		{
			// TODO this entire thing....
			/*
			 * It might need to incorporate deltaTime somehow, although that's not very important It looks *okay* when grabbling people, but seriously buggy when grabbling a ball Why does the ball just constantly become faster and faster and
			 * stretches more and more???
			 */

			double maxSpringiness = 40; // If it's too high (or doesn't exist), repeated rotation of vine will constantly escalate the delta length for some reason, and physics will wackify.
			double springiness = v.deltaLength;
			if (springiness < -maxSpringiness)
				springiness = -maxSpringiness;
			if (springiness > maxSpringiness)
				springiness = maxSpringiness;

			// pulling/pushing like a spring: F = k*delta
			v.creator.xVel -= Math.cos(v.rotation) * deltaTime * springiness * v.rigidity / v.creator.mass / 2;// functionally has twice the mass
			v.creator.yVel -= Math.sin(v.rotation) * deltaTime * springiness * v.rigidity / v.creator.mass / 2;//
			v.grabbledThing.xVel += Math.cos(v.rotation) * deltaTime * springiness * v.rigidity / v.grabbledThing.mass;
			v.grabbledThing.yVel += Math.sin(v.rotation) * deltaTime * springiness * v.rigidity / v.grabbledThing.mass;

			// Pulling vine sideways
			double difference = desiredAngle - v.rotation;
			while (difference < -TAU / 2)
				difference += TAU;
			while (difference > TAU / 2)
				difference -= TAU;
			double circularAccel = v.spinStrength / (v.grabbledThing.mass * Math.max(v.length + v.deltaLength, 200)); // when vine is shorter than ~2 meters, it isn't infinitely easy to wrangle.
			if (difference < 0)
				circularAccel *= -1;
			v.grabbledThing.xVel += circularAccel * deltaTime * Math.cos(v.rotation + TAU / 4);
			v.grabbledThing.yVel += circularAccel * deltaTime * Math.sin(v.rotation + TAU / 4);

			if (v.grabbledThing instanceof Ball)
				if (v.grabbledThing.velocity() > 500)
					v.grabbledThing.zVel = 0;
		}
		if (v.state == 0 || v.state == 2)
		{
			if (v.state == 0) // checking retraction
			{
				if (v.length > v.range - v.startDistance)
					v.retract();
				if (v.length > Math.sqrt(Methods.DistancePow2(v.creator.target.x, v.creator.target.y, v.start.x, v.start.y))) // retracts if it reaches mouse point
					v.retract(); // TODO add a settings-menu option to disable this
			}

			double vineSpeed = 20;
			if (v.state == 2) // retracting
				vineSpeed = -vineSpeed;
			// Pulling vine sideways
			v.rotate(desiredAngle, deltaTime);
			if (v.endPauseTimeLeft == 0)
			{
				if (!v.retractionSound)
				{
					v.creatorAbility.sounds.get(1).play();
					v.retractionSound = true;
				}
				double angle = Math.atan2(v.end.y - v.start.y, v.end.x - v.start.x);
				v.end.x += vineSpeed * Math.cos(angle);
				v.end.y += vineSpeed * Math.sin(angle);
			}
			else if (v.endPauseTimeLeft < 0)
				v.endPauseTimeLeft = 0;
			else
				v.endPauseTimeLeft -= deltaTime;
		}

		Line2D vineLine = new Line2D.Double(v.start.x, v.start.y, v.end.x, v.end.y);

		double shortestDistancePow2 = Double.MAX_VALUE;

		int collisionType = -1;
		Point2D intersectionPoint = null;

		// 0 walls
		int lowestGridX = Math.min(v.start.x, v.end.x) / squareSize;
		int lowestGridY = Math.min(v.start.y, v.end.y) / squareSize;
		int highestGridX = Math.max(v.start.x, v.end.x) / squareSize;
		int highestGridY = Math.max(v.start.y, v.end.y) / squareSize;

		Point collidedWall = null;
		if (v.z - v.height / 2 < 1)
			for (int x = lowestGridX; x <= highestGridX; x++)
				for (int y = lowestGridY; y <= highestGridY; y++)
					if (wallTypes[x][y] != -1)
					{
						Rectangle2D wallRect = new Rectangle2D.Double(x * squareSize, y * squareSize, squareSize, squareSize);
						// if they intersect
						if (vineLine.intersects(wallRect))
						{
							// find point of intersection (and side)
							List<Line2D> lines = new ArrayList<Line2D>();
							lines.add(new Line2D.Double(wallRect.getX(), wallRect.getY(), wallRect.getX() + wallRect.getWidth(), wallRect.getY()));
							lines.add(new Line2D.Double(wallRect.getX(), wallRect.getY(), wallRect.getX(), wallRect.getY() + wallRect.getWidth()));
							lines.add(new Line2D.Double(wallRect.getX() + wallRect.getWidth(), wallRect.getY(), wallRect.getX() + wallRect.getWidth(), wallRect.getY() + wallRect.getWidth()));
							lines.add(new Line2D.Double(wallRect.getX(), wallRect.getY() + wallRect.getWidth(), wallRect.getX() + wallRect.getWidth(), wallRect.getY() + wallRect.getWidth()));

							for (int i = 0; i < lines.size(); i++)
								if (!lines.get(i).intersectsLine(vineLine))
								{
									lines.remove(i);
									i--;
								}
							// finding closest point on rectangle
							Point2D intersectionP = null;
							for (int i = 0; i < lines.size(); i++)
							{
								Point2D intersection = Methods.getLineLineIntersection(lines.get(i), vineLine);
								if (intersection != null)
								{
									double distancePow2 = Methods.DistancePow2(v.start, intersection);
									if (distancePow2 < shortestDistancePow2)
									{
										intersectionP = intersection;
										shortestDistancePow2 = distancePow2;
									}
									else
									{
										lines.remove(i);
										i--;
									}
								}
								else
								{
									lines.remove(i);
									i--;
								}
							}
							if (intersectionP != null)
							{
								collisionType = 0;
								intersectionPoint = intersectionP;
								collidedWall = new Point(x, y);
							}
						}
					}

		// 1 ffs
		ForceField collidedFF = null;
		for (ForceField ff : FFs)
			if (v.z - v.height / 2 < ff.z + ff.height || v.z + v.height / 2 > ff.z)
			{

				// find point of intersection (and side)
				List<Line2D> lines = new ArrayList<Line2D>();

				for (int j = 0; j < ff.p.length - 1; j++)
					lines.add(new Line2D.Double(ff.p[j], ff.p[j + 1]));
				lines.add(new Line2D.Double(ff.p[ff.p.length - 1], ff.p[0]));
				if (!lines.isEmpty())

					for (int i = 0; i < lines.size(); i++)
						if (!lines.get(i).intersectsLine(vineLine))
						{
							lines.remove(i);
							i--;
						}
				// finding closest point
				Point2D intersectionP = null;
				for (int i = 0; i < lines.size(); i++)
				{
					Point2D intersection = Methods.getLineLineIntersection(lines.get(i), vineLine);
					if (intersection != null)
					{
						double distancePow2 = Methods.DistancePow2(v.start, intersection);
						if (distancePow2 < shortestDistancePow2)
						{
							intersectionP = intersection;
							shortestDistancePow2 = distancePow2;
							intersectionPoint = intersection;
						}
						else
						{
							lines.remove(i);
							i--;
						}
					}
					else
					{
						lines.remove(i);
						i--;
					}
				}
				if (intersectionP != null)
				{
					collisionType = 1;
					collidedFF = ff;
				}

			}

		// 2 arcffs/
		ArcForceField collidedAFF = null;
		for (ArcForceField aff : AFFs)
			if (v.z - v.height / 2 < aff.z + aff.height || v.z + v.height / 2 > aff.z)
			{

				Rectangle2D generalBounds = new Rectangle2D.Double(aff.x - aff.maxRadius, aff.y - aff.maxRadius, aff.maxRadius * 2, aff.maxRadius * 2);
				// easier intersection first
				if (vineLine.intersects(generalBounds))
				{
					// detailed intersection
					double minAngle = aff.rotation - aff.arc / 2;
					double maxAngle = aff.rotation + aff.arc / 2;

					while (minAngle < 0)
						minAngle += 2 * Math.PI;
					while (minAngle >= 2 * Math.PI)
						minAngle -= 2 * Math.PI;
					while (maxAngle < 0)
						maxAngle += 2 * Math.PI;
					while (maxAngle >= 2 * Math.PI)
						maxAngle -= 2 * Math.PI;

					Point2D closestPointToSegment = Methods.getClosestPointOnSegment(vineLine.getX1(), vineLine.getY1(), vineLine.getX2(), vineLine.getY2(), aff.x, aff.y);

					List<Point2D> points = new ArrayList<Point2D>();
					List<Line2D> lines = new ArrayList<Line2D>();

					for (int k = -1; k < 2; k += 2) // intended to check both intersections of the line with the circle
					{
						double closestPointDistanceMax = Math.sqrt(Methods.DistancePow2(closestPointToSegment.getX(), closestPointToSegment.getY(), aff.x, aff.y));
						double angleToCollisionPointMax = Math.atan2(closestPointToSegment.getY() - aff.y, closestPointToSegment.getX() - aff.x)
								+ k * Math.acos(closestPointDistanceMax / aff.maxRadius);

						double closestPointDistanceMin = Math.sqrt(Methods.DistancePow2(closestPointToSegment.getX(), closestPointToSegment.getY(), aff.x, aff.y));
						double angleToCollisionPointMin = Math.atan2(closestPointToSegment.getY() - aff.y, closestPointToSegment.getX() - aff.x)
								+ k * Math.acos(closestPointDistanceMin / aff.minRadius);

						Point2D closestPointMax = new Point2D.Double(aff.x + aff.maxRadius * Math.cos(angleToCollisionPointMax), aff.y + aff.maxRadius * Math.sin(angleToCollisionPointMax));
						Point2D closestPointMin = new Point2D.Double(aff.x + aff.minRadius * Math.cos(angleToCollisionPointMin), aff.y + aff.minRadius * Math.sin(angleToCollisionPointMin));

						while (angleToCollisionPointMax < 0)
							angleToCollisionPointMax += 2 * Math.PI;
						while (angleToCollisionPointMax >= 2 * Math.PI)
							angleToCollisionPointMax -= 2 * Math.PI;

						while (angleToCollisionPointMin < 0)
							angleToCollisionPointMin += 2 * Math.PI;
						while (angleToCollisionPointMin >= 2 * Math.PI)
							angleToCollisionPointMin -= 2 * Math.PI;

						// outer arc
						if (closestPointDistanceMax < aff.maxRadius)
							if (minAngle < maxAngle)
							{
								if (angleToCollisionPointMax > minAngle && angleToCollisionPointMax < maxAngle)
								{
									points.add(closestPointMax);
									lines.add(new Line2D.Double(closestPointMax.getX() + 12 * Math.cos(angleToCollisionPointMax + Math.PI / 2),
											closestPointMax.getY() + 12 * Math.sin(angleToCollisionPointMax + Math.PI / 2),
											closestPointMax.getX() - 12 * Math.cos(angleToCollisionPointMax + Math.PI / 2),
											closestPointMax.getY() - 12 * Math.sin(angleToCollisionPointMax + Math.PI / 2)));
								}
							}
							else if (angleToCollisionPointMax < maxAngle || angleToCollisionPointMax > minAngle)
							{
								points.add(closestPointMax);
								lines.add(new Line2D.Double(closestPointMax.getX() + 12 * Math.cos(angleToCollisionPointMax + Math.PI / 2),
										closestPointMax.getY() + 12 * Math.sin(angleToCollisionPointMax + Math.PI / 2), closestPointMax.getX() - 12 * Math.cos(angleToCollisionPointMax + Math.PI / 2),
										closestPointMax.getY() - 12 * Math.sin(angleToCollisionPointMax + Math.PI / 2)));

							}
						// inner arc
						if (closestPointDistanceMin < aff.minRadius)
							if (minAngle < maxAngle)
							{
								if (angleToCollisionPointMin > minAngle && angleToCollisionPointMin < maxAngle)
								{
									points.add(closestPointMin);
									lines.add(new Line2D.Double(closestPointMin.getX() + 12 * Math.cos(angleToCollisionPointMin + Math.PI / 2),
											closestPointMin.getY() + 12 * Math.sin(angleToCollisionPointMin + Math.PI / 2),
											closestPointMin.getX() - 12 * Math.cos(angleToCollisionPointMin + Math.PI / 2),
											closestPointMin.getY() - 12 * Math.sin(angleToCollisionPointMin + Math.PI / 2)));
								}
							}
							else if (angleToCollisionPointMin < maxAngle || angleToCollisionPointMin > minAngle)
							{
								points.add(closestPointMin);
								lines.add(new Line2D.Double(closestPointMin.getX() + 12 * Math.cos(angleToCollisionPointMin + Math.PI / 2),
										closestPointMin.getY() + 12 * Math.sin(angleToCollisionPointMin + Math.PI / 2), closestPointMin.getX() - 12 * Math.cos(angleToCollisionPointMin + Math.PI / 2),
										closestPointMin.getY() - 12 * Math.sin(angleToCollisionPointMin + Math.PI / 2)));
							}
					}
					// two sides:
					Line2D l1 = new Line2D.Double(aff.x + aff.minRadius * Math.cos(aff.rotation - 0.5 * aff.arc), aff.y + aff.minRadius * Math.sin(aff.rotation - 0.5 * aff.arc),
							aff.x + aff.maxRadius * Math.cos(aff.rotation - 0.5 * aff.arc), aff.y + aff.maxRadius * Math.sin(aff.rotation - 0.5 * aff.arc));
					Line2D l2 = new Line2D.Double(aff.x + aff.minRadius * Math.cos(aff.rotation + 0.5 * aff.arc), aff.y + aff.minRadius * Math.sin(aff.rotation + 0.5 * aff.arc),
							aff.x + aff.maxRadius * Math.cos(aff.rotation + 0.5 * aff.arc), aff.y + aff.maxRadius * Math.sin(aff.rotation + 0.5 * aff.arc));
					lines.add(l1);
					points.add(Methods.getSegmentIntersection(l1, vineLine));
					lines.add(l2);
					points.add(Methods.getSegmentIntersection(l2, vineLine));
					// finding closest point
					Point2D intersectionP = null;
					for (int i = 0; i < points.size(); i++)
					{
						Point2D intersection = points.get(i);
						if (intersection != null)
						{
							double distancePow2 = Methods.DistancePow2(v.start, intersection);
							if (distancePow2 < shortestDistancePow2)
							{
								intersectionP = intersection;
								shortestDistancePow2 = distancePow2;
							}
							else
							{
								points.remove(i);
								lines.remove(i);
								i--;
							}
						}
						else
						{
							points.remove(i);
							lines.remove(i);
							i--;
						}
					}
					if (intersectionP != null)
					{
						collisionType = 2;
						collidedAFF = aff;
						intersectionPoint = intersectionP;
					}

				}
			}

		// 3,4 persons
		Person collidedPerson = null;
		peopleLoop: for (Person p : people)
			if (p.id != v.creator.id)
			{
				for (Evasion e : v.evasions)
					if (e.id == p.id)
						continue peopleLoop;
				// grabbling test
				if (v.state == 0 || v.state == 2)
					if (Math.abs(p.z - v.end.z) < 2 && Methods.DistancePow2(p.x, p.y, v.end.x, v.end.y) < Vine.grabblingRange * Vine.grabblingRange) // z difference 2? TODO
					{
						collisionType = 3;
						collidedPerson = p;
						intersectionPoint = new Point2D.Double(-1, -1); // irrelevant
					}

				// collision with the body (length) of the vine
				if (v.state != 1 && !v.creator.equals(v.grabbledThing))
					if (Methods.SegmentToPointDistancePow2(v.start.Point(), v.end.Point(), p.Point()) <= p.radius * p.radius / 4)
					{
						Point closestPoint = Methods.getClosestRoundedPointOnSegment(v.start.Point(), v.end.Point(), p.Point());
						double distancePow2 = Methods.DistancePow2(p.Point(), closestPoint);
						if (distancePow2 < shortestDistancePow2)
						{
							shortestDistancePow2 = distancePow2;
							collisionType = 4;
							collidedPerson = p;
							intersectionPoint = new Point2D.Double(-1, -1); // irrelevant
						}
					}
			}

		// 5,6 balls
		Ball collidedBall = null;
		for (Ball b : balls)
		{
			// grabbling test
			if (v.state == 0 || v.state == 2)
				if (Math.abs(b.z - v.end.z) < 2 && Methods.DistancePow2(b.x, b.y, v.end.x, v.end.y) < Vine.grabblingRange * Vine.grabblingRange)
				{
					collisionType = 5;
					collidedBall = b;
					intersectionPoint = new Point2D.Double(-1, -1); // irrelevant
				}

			// collision with the body (length) of the vine
			if (v.state != 1)
				if (Methods.SegmentToPointDistancePow2(v.start.Point(), v.end.Point(), b.Point()) <= b.radius * b.radius)
				{
					Point closestPoint = Methods.getClosestRoundedPointOnSegment(v.start.Point(), v.end.Point(), b.Point());
					double distancePow2 = Methods.DistancePow2(b.Point(), closestPoint);
					if (distancePow2 < shortestDistancePow2)
					{
						shortestDistancePow2 = distancePow2;
						collisionType = 6;
						collidedBall = b;
						intersectionPoint = new Point2D.Double(-1, -1); // irrelevant
					}
				}
		}

		// 7 Portals
		Portal collidedPortal = null;
		for (Portal p : portals)
			if (v.z < p.highestPoint() && v.highestPoint() > p.z)
				if (vineLine.intersectsLine(p.Line2D()))
				{
					collidedPortal = p;
					collisionType = 7;
					intersectionPoint = Methods.getLineLineIntersection(collidedPortal.Line2D(), vineLine);
				}

		if (collisionType == -1)
			return;
		if (intersectionPoint == null)
		{
			MAIN.errorMessage("what what what what? Um");
			return;
		}
		Point roundedIntersectionPoint = new Point((int) intersectionPoint.getX(), (int) intersectionPoint.getY());

		switch (collisionType)
		{
		case 0: // wall
			// TODO Create plant debris / cut-off plant between intersection point and vine end
			v.grabbledThing = null;

			v.end.x = roundedIntersectionPoint.x;
			v.end.y = roundedIntersectionPoint.y;
			if (v.state != 2)
				v.retract();
			v.fixPosition();
			damageWall(collidedWall.x, collidedWall.y, v.damage, EP.damageType(EP.toInt("Plant")));
			break;
		case 1: // force field
			v.end.x = roundedIntersectionPoint.x;
			v.end.y = roundedIntersectionPoint.y;
			if (v.state != 2)
				v.retract();
			v.fixPosition();
			damageForceField(collidedFF, v.damage, roundedIntersectionPoint);
			break;
		case 2: // arc force field
			v.end.x = roundedIntersectionPoint.x;
			v.end.y = roundedIntersectionPoint.y;
			if (v.state != 2)
				v.retract();
			v.fixPosition();
			damageArcForceField(collidedAFF, v.damage, roundedIntersectionPoint, EP.damageType("Plant"));
			break;
		case 3: // person (grabbled)
			if (checkForEvasion(collidedPerson))
				v.evadedBy(collidedPerson);
			else
			{
				v.end = new Point3D((int) collidedPerson.x, (int) collidedPerson.y, (int) collidedPerson.z);
				v.grabbledThing = collidedPerson;
				v.state = 1;
				v.fixPosition();

				// damage
				hitPerson(collidedPerson, v.damage * deltaTime, 0, 0, 11, deltaTime);
			}
			break;
		case 4: // person (intersected)
			if (checkForEvasion(collidedPerson))
				v.evadedBy(collidedPerson);
			else
			{
				v.rotate(originalAngle, deltaTime * 2);
				if (v.state == 1)
				{
					double difference = desiredAngle - v.rotation;
					while (difference < -TAU / 2)
						difference += TAU;
					while (difference > TAU / 2)
						difference -= TAU;
					double hitAngle = v.rotation + (difference > 0 ? TAU / 4 : -TAU / 4);
					double vineCollisionPercentage = 0.01;
					double push = Math.min(Math.sqrt(Math.pow(v.grabbledThing.xVel, 2) + Math.pow(v.grabbledThing.yVel, 2)) * vineCollisionPercentage, v.getCollisionPushback());
					hitPerson(collidedPerson, v.getCollisionDamage(), push, hitAngle, 11);
					collidedPerson.rotation = hitAngle;
					collidedPerson.slip(true);
				}
				v.fixPosition();
			}
			break;
		case 5: // ball (grabbled)
			v.end = new Point3D((int) collidedBall.x, (int) collidedBall.y, (int) collidedBall.z);
			v.grabbledThing = collidedBall;
			v.state = 1;
			v.fixPosition();
			break;
		case 6: // ball (intersected)
			v.rotate(originalAngle, deltaTime * 2);
			v.fixPosition();
			break;
		case 7: // portal
			v.retract();
			v.end.x = roundedIntersectionPoint.x;
			v.end.y = roundedIntersectionPoint.y;
			if (v.state != 2)
				v.retract();
			v.fixPosition();
			break;
		default:
			MAIN.errorMessage("tutorial times 4: how to write an error message 404");
			break;
		}
	}

	public void moveBeam(Beam b, boolean recursive, double deltaTime) // Recursive
	{
		double angle = Math.atan2(b.end.y - b.start.y, b.end.x - b.start.x);
		// move it slightly forward
		b.end.x += 3 * Math.cos(angle);
		b.end.y += 3 * Math.sin(angle);
		Line2D beamLine = new Line2D.Double(b.start.x, b.start.y, b.end.x, b.end.y);

		// Preliminary collision testing, in order to reduce the number of collisions to 1
		double shortestDistancePow2 = Double.MAX_VALUE;
		Line2D collisionLine = null;
		int collisionType = -1; // -1 = none, 0 = wall, 1 = force field, 2 = arc force field, 3 = person, 4 = ball

		// walls
		Point collidedWall = null;
		if (b.z - b.height / 2 < 1)
			for (int x = 0; x < width; x++)
				for (int y = 0; y < width; y++)
					if (wallTypes[x][y] != -1)
					{
						Rectangle2D wallRect = new Rectangle2D.Double(x * squareSize, y * squareSize, squareSize, squareSize);
						// if they intersect
						if (beamLine.intersects(wallRect))
						{
							// find point of intersection (and side)
							List<Line2D> lines = new ArrayList<Line2D>();
							lines.add(new Line2D.Double(wallRect.getX(), wallRect.getY(), wallRect.getX() + wallRect.getWidth(), wallRect.getY()));
							lines.add(new Line2D.Double(wallRect.getX(), wallRect.getY(), wallRect.getX(), wallRect.getY() + wallRect.getWidth()));
							lines.add(new Line2D.Double(wallRect.getX() + wallRect.getWidth(), wallRect.getY(), wallRect.getX() + wallRect.getWidth(), wallRect.getY() + wallRect.getWidth()));
							lines.add(new Line2D.Double(wallRect.getX(), wallRect.getY() + wallRect.getWidth(), wallRect.getX() + wallRect.getWidth(), wallRect.getY() + wallRect.getWidth()));

							for (int i = 0; i < lines.size(); i++)
								if (!lines.get(i).intersectsLine(beamLine))
								{
									lines.remove(i);
									i--;
								}
							// finding closest point on rectangle
							Point2D intersectionP = null;
							for (int i = 0; i < lines.size(); i++)
							{
								Point2D intersection = Methods.getLineLineIntersection(lines.get(i), beamLine);
								if (intersection != null)
								{
									double distancePow2 = Methods.DistancePow2(b.start, intersection);
									if (distancePow2 < shortestDistancePow2)
									{
										intersectionP = intersection;
										shortestDistancePow2 = distancePow2;
										collisionLine = lines.get(i);
									}
									else
									{
										lines.remove(i);
										i--;
									}
								}
								else
								{
									lines.remove(i);
									i--;
								}
							}
							if (intersectionP != null)
							{
								collisionType = 0;
								collidedWall = new Point(x, y);
							}
						}
					}

		// forcefields
		ForceField collidedFF = null;
		for (ForceField ff : FFs)
		{
			if (b.z - b.height / 2 < ff.z + ff.height && b.z + b.height / 2 > ff.z)
			{
				Rectangle2D generalBounds = new Rectangle2D.Double(ff.x - ff.length / 2 - ff.width / 2, ff.y - ff.length / 2 - ff.width / 2, ff.length + ff.width, ff.length + ff.width);
				// easier intersection first
				if (beamLine.intersects(generalBounds))
				{
					// detailed intersection
					List<Line2D> lines = new ArrayList<Line2D>();
					for (int j = 0; j < ff.p.length - 1; j++)
						lines.add(new Line2D.Double(ff.p[j], ff.p[j + 1]));
					lines.add(new Line2D.Double(ff.p[ff.p.length - 1], ff.p[0]));
					if (!lines.isEmpty())

						for (int i = 0; i < lines.size(); i++)
							if (!lines.get(i).intersectsLine(beamLine))
							{
								lines.remove(i);
								i--;
							}
					// finding closest point
					Point2D intersectionP = null;
					for (int i = 0; i < lines.size(); i++)
					{
						Point2D intersection = Methods.getLineLineIntersection(lines.get(i), beamLine);
						if (intersection != null)
						{
							double distancePow2 = Methods.DistancePow2(b.start, intersection);
							if (distancePow2 < shortestDistancePow2)
							{
								intersectionP = intersection;
								shortestDistancePow2 = distancePow2;
								collisionLine = lines.get(i);
							}
							else
							{
								lines.remove(i);
								i--;
							}
						}
						else
						{
							lines.remove(i);
							i--;
						}
					}
					if (intersectionP != null)
					{
						collisionType = 1;
						collidedFF = ff;
					}
				}
			}
		}

		// arc force fields
		ArcForceField collidedAFF = null;
		for (ArcForceField aff : AFFs)
		{
			if (b.z - b.height / 2 < aff.z + aff.height && b.z + b.height / 2 > aff.z) // height check
				if (Methods.DistancePow2(b.start, aff.Point()) > aff.maxRadius * aff.maxRadius) // if beam did not originate inside arc force field.
				{
					Rectangle2D generalBounds = new Rectangle2D.Double(aff.x - aff.maxRadius, aff.y - aff.maxRadius, aff.maxRadius * 2, aff.maxRadius * 2);
					// easier intersection first
					if (beamLine.intersects(generalBounds))
					{
						List<Point2D> points = new ArrayList<Point2D>();
						List<Line2D> lines = new ArrayList<Line2D>();
						if (aff.arc < TAU)
						{
							// detailed intersection
							double minAngle = aff.rotation - aff.arc / 2;
							double maxAngle = aff.rotation + aff.arc / 2;

							while (minAngle < 0)
								minAngle += 2 * Math.PI;
							while (minAngle >= 2 * Math.PI)
								minAngle -= 2 * Math.PI;
							while (maxAngle < 0)
								maxAngle += 2 * Math.PI;
							while (maxAngle >= 2 * Math.PI)
								maxAngle -= 2 * Math.PI;

							Point2D closestPointToSegment = Methods.getClosestPointOnSegment(beamLine.getX1(), beamLine.getY1(), beamLine.getX2(), beamLine.getY2(), aff.x, aff.y);

							for (int k = -1; k < 2; k += 2) // intended to check both intersections of the line with the circle
							{
								double closestPointDistanceMax = Math.sqrt(Methods.DistancePow2(closestPointToSegment.getX(), closestPointToSegment.getY(), aff.x, aff.y));
								double angleToCollisionPointMax = Math.atan2(closestPointToSegment.getY() - aff.y, closestPointToSegment.getX() - aff.x)
										+ k * Math.acos(closestPointDistanceMax / aff.maxRadius);

								double closestPointDistanceMin = Math.sqrt(Methods.DistancePow2(closestPointToSegment.getX(), closestPointToSegment.getY(), aff.x, aff.y));
								double angleToCollisionPointMin = Math.atan2(closestPointToSegment.getY() - aff.y, closestPointToSegment.getX() - aff.x)
										+ k * Math.acos(closestPointDistanceMin / aff.minRadius);

								Point2D closestPointMax = new Point2D.Double(aff.x + aff.maxRadius * Math.cos(angleToCollisionPointMax), aff.y + aff.maxRadius * Math.sin(angleToCollisionPointMax));
								Point2D closestPointMin = new Point2D.Double(aff.x + aff.minRadius * Math.cos(angleToCollisionPointMin), aff.y + aff.minRadius * Math.sin(angleToCollisionPointMin));

								while (angleToCollisionPointMax < 0)
									angleToCollisionPointMax += 2 * Math.PI;
								while (angleToCollisionPointMax >= 2 * Math.PI)
									angleToCollisionPointMax -= 2 * Math.PI;

								while (angleToCollisionPointMin < 0)
									angleToCollisionPointMin += 2 * Math.PI;
								while (angleToCollisionPointMin >= 2 * Math.PI)
									angleToCollisionPointMin -= 2 * Math.PI;

								// outer arc
								if (closestPointDistanceMax < aff.maxRadius)
									if (minAngle < maxAngle)
									{
										if (angleToCollisionPointMax > minAngle && angleToCollisionPointMax < maxAngle)
										{
											points.add(closestPointMax);
											lines.add(new Line2D.Double(closestPointMax.getX() + 12 * Math.cos(angleToCollisionPointMax + Math.PI / 2),
													closestPointMax.getY() + 12 * Math.sin(angleToCollisionPointMax + Math.PI / 2),
													closestPointMax.getX() - 12 * Math.cos(angleToCollisionPointMax + Math.PI / 2),
													closestPointMax.getY() - 12 * Math.sin(angleToCollisionPointMax + Math.PI / 2)));
										}
									}
									else if (angleToCollisionPointMax < maxAngle || angleToCollisionPointMax > minAngle)
									{
										points.add(closestPointMax);
										lines.add(new Line2D.Double(closestPointMax.getX() + 12 * Math.cos(angleToCollisionPointMax + Math.PI / 2),
												closestPointMax.getY() + 12 * Math.sin(angleToCollisionPointMax + Math.PI / 2),
												closestPointMax.getX() - 12 * Math.cos(angleToCollisionPointMax + Math.PI / 2),
												closestPointMax.getY() - 12 * Math.sin(angleToCollisionPointMax + Math.PI / 2)));

									}
								// inner arc
								if (closestPointDistanceMin < aff.minRadius)
									if (minAngle < maxAngle)
									{
										if (angleToCollisionPointMin > minAngle && angleToCollisionPointMin < maxAngle)
										{
											points.add(closestPointMin);
											lines.add(new Line2D.Double(closestPointMin.getX() + 12 * Math.cos(angleToCollisionPointMin + Math.PI / 2),
													closestPointMin.getY() + 12 * Math.sin(angleToCollisionPointMin + Math.PI / 2),
													closestPointMin.getX() - 12 * Math.cos(angleToCollisionPointMin + Math.PI / 2),
													closestPointMin.getY() - 12 * Math.sin(angleToCollisionPointMin + Math.PI / 2)));
										}
									}
									else if (angleToCollisionPointMin < maxAngle || angleToCollisionPointMin > minAngle)
									{
										points.add(closestPointMin);
										lines.add(new Line2D.Double(closestPointMin.getX() + 12 * Math.cos(angleToCollisionPointMin + Math.PI / 2),
												closestPointMin.getY() + 12 * Math.sin(angleToCollisionPointMin + Math.PI / 2),
												closestPointMin.getX() - 12 * Math.cos(angleToCollisionPointMin + Math.PI / 2),
												closestPointMin.getY() - 12 * Math.sin(angleToCollisionPointMin + Math.PI / 2)));
									}
							}
							// two sides:
							Line2D l1 = new Line2D.Double(aff.x + aff.minRadius * Math.cos(aff.rotation - 0.5 * aff.arc), aff.y + aff.minRadius * Math.sin(aff.rotation - 0.5 * aff.arc),
									aff.x + aff.maxRadius * Math.cos(aff.rotation - 0.5 * aff.arc), aff.y + aff.maxRadius * Math.sin(aff.rotation - 0.5 * aff.arc));
							Line2D l2 = new Line2D.Double(aff.x + aff.minRadius * Math.cos(aff.rotation + 0.5 * aff.arc), aff.y + aff.minRadius * Math.sin(aff.rotation + 0.5 * aff.arc),
									aff.x + aff.maxRadius * Math.cos(aff.rotation + 0.5 * aff.arc), aff.y + aff.maxRadius * Math.sin(aff.rotation + 0.5 * aff.arc));
							lines.add(l1);
							points.add(Methods.getSegmentIntersection(l1, beamLine));
							lines.add(l2);
							points.add(Methods.getSegmentIntersection(l2, beamLine));
						}
						else // much easier
						if (Methods.SegmentToPointDistancePow2(b.start.Point(), b.end.Point(), aff.Point()) < aff.maxRadius * aff.maxRadius)
						{
							Point2D closestPointToSegment = Methods.getClosestPointOnSegment(beamLine.getX1(), beamLine.getY1(), beamLine.getX2(), beamLine.getY2(), aff.x, aff.y);

							for (int k = -1; k < 2; k += 2) // intended to check both intersections of the line with the circle
							{
								double closestPointDistanceMax = Math.sqrt(Methods.DistancePow2(closestPointToSegment.getX(), closestPointToSegment.getY(), aff.x, aff.y));
								double angleToCollisionPointMax = Math.atan2(closestPointToSegment.getY() - aff.y, closestPointToSegment.getX() - aff.x)
										+ k * Math.acos(closestPointDistanceMax / aff.maxRadius);

								Point2D closestPointMax = new Point2D.Double(aff.x + aff.maxRadius * Math.cos(angleToCollisionPointMax), aff.y + aff.maxRadius * Math.sin(angleToCollisionPointMax));

								// outer circle
								if (closestPointDistanceMax < aff.maxRadius)
								{
									points.add(closestPointMax);
									lines.add(new Line2D.Double(closestPointMax.getX() + 12 * Math.cos(angleToCollisionPointMax + Math.PI / 2),
											closestPointMax.getY() + 12 * Math.sin(angleToCollisionPointMax + Math.PI / 2),
											closestPointMax.getX() - 12 * Math.cos(angleToCollisionPointMax + Math.PI / 2),
											closestPointMax.getY() - 12 * Math.sin(angleToCollisionPointMax + Math.PI / 2)));
								}
							}
						}
						// finding closest point
						Point2D intersectionP = null;
						for (int i = 0; i < points.size(); i++)
						{
							Point2D intersection = points.get(i);
							if (intersection != null)
							{
								double distancePow2 = Methods.DistancePow2(b.start, intersection);
								if (distancePow2 < shortestDistancePow2)
								{
									intersectionP = intersection;
									shortestDistancePow2 = distancePow2;
									collisionLine = lines.get(i);
								}
								else
								{
									points.remove(i);
									lines.remove(i);
									i--;
								}
							}
							else
							{
								points.remove(i);
								lines.remove(i);
								i--;
							}
						}
						if (intersectionP != null)
						{
							collisionType = 2;
							collidedAFF = aff;
						}
					}
				}
		}

		// People
		Person collidedPerson = null;
		peopleLoop: for (Person p : people)
		{
			for (Evasion e : b.theAbility.evasions)
				if (e.id == p.id)
					continue peopleLoop;
			if (p.equals(b.creator) && !b.isChild)
				continue peopleLoop;
			if (p.z <= b.z + b.height / 2 && p.highestPoint() > b.z - b.height / 2) // height check
			{
				Point2D intersectionP = Methods.getClosestPointOnSegment(b.start.x, b.start.y, b.end.x, b.end.y, p.x, p.y);
				if (Methods.DistancePow2(intersectionP.getX(), intersectionP.getY(), p.x, p.y) < Math.pow(p.radius + b.size * b.radius, 2)) // intersection check
				{
					double distancePow2 = Methods.DistancePow2(b.start, intersectionP);
					if (distancePow2 < shortestDistancePow2)
					{
						shortestDistancePow2 = distancePow2;
						collisionLine = new Line2D.Double(intersectionP.getX() + 100 * Math.cos(angle + Math.PI / 2), intersectionP.getY() + 100 * Math.sin(angle + Math.PI / 2),
								intersectionP.getX() - 100 * Math.cos(angle + Math.PI / 2), intersectionP.getY() - 100 * Math.sin(angle + Math.PI / 2));
						collisionType = 3;
						collidedPerson = p;
					}
				}
			}
		}

		// Balls - 4
		Ball collidedBall = null;
		for (Ball ball : balls)
		{
			if (b.z + b.height / 2 > ball.z - b.height / 2 && b.z - b.height / 2 < ball.z + b.height / 2)
			{
				double distancePow2 = Methods.SegmentToPointDistancePow2(new Point(b.start.x, b.start.y), new Point(b.end.x, b.end.y), new Point((int) ball.x, (int) ball.y));
				if (distancePow2 < Math.pow(ball.radius + b.size * b.radius, 2))
				{
					Point2D intersectionP = null;
					double realDistancePow2 = Methods.DistancePow2(b.start, new Point((int) ball.x, (int) ball.y));
					if (realDistancePow2 < shortestDistancePow2)
					{
						intersectionP = new Point2D.Double(b.start.x + Math.sqrt(realDistancePow2) * Math.cos(angle), b.start.y + Math.sqrt(realDistancePow2) * Math.sin(angle));
						shortestDistancePow2 = realDistancePow2;
						collisionLine = new Line2D.Double(intersectionP.getX() + 100 * Math.cos(angle + Math.PI / 2), intersectionP.getY() + 100 * Math.sin(angle + Math.PI / 2),
								intersectionP.getX() - 100 * Math.cos(angle + Math.PI / 2), intersectionP.getY() - 100 * Math.sin(angle + Math.PI / 2));
						collisionType = 4;
						collidedBall = ball;
					}
				}
			}
		}

		// Vines - 5
		Vine collidedVine = null;
		for (Vine v : vines)
		{
			if (b.z + b.height / 2 > v.z - b.height / 2 && b.z - b.height / 2 < v.z + b.height / 2)
			{
				Line2D vineLine = new Line2D.Double(v.start.x, v.start.y, v.end.x, v.end.y);
				Point2D intersection = Methods.getSegmentIntersection(beamLine, vineLine);
				if (intersection != null) // easiest collision detection so far! \o/
				{
					collisionLine = new Line2D.Double(intersection.getX() + 100 * Math.cos(angle + Math.PI / 2), intersection.getY() + 100 * Math.sin(angle + Math.PI / 2),
							intersection.getX() - 100 * Math.cos(angle + Math.PI / 2), intersection.getY() - 100 * Math.sin(angle + Math.PI / 2));
					collisionType = 5;
					collidedVine = v;
				}
			}
		}

		// Portals - 6
		Portal collidedPortal = null;
		double minimumDistanceFromStart = 10; // to avoid post-portal beams hitting their own exit portal
		for (Portal p : portals)
		{
			if (p.partner != null && p.highestPoint() > b.z && b.highestPoint() > p.z && beamLine.intersectsLine(p.Line2D()))
			{
				Point2D intersection = Methods.getSegmentIntersection(beamLine, p.Line2D());
				if (intersection != null)
				{
					double distPow2 = Methods.DistancePow2(b.start, intersection);
					if (distPow2 > minimumDistanceFromStart * minimumDistanceFromStart)
						if (distPow2 < shortestDistancePow2)
						{
							shortestDistancePow2 = distPow2;
							collisionLine = p.Line2D();
							collisionType = 6;
							collidedPortal = p;
						}
				}
			}
		}

		if (collisionType == -1)
		{
			b.endType = 0;
			b.endAngle = angle;
			// collide with environment edge
			Line2D left = new Line2D.Double(0, 0, 0, heightPixels);
			Line2D up = new Line2D.Double(0, 0, widthPixels, 0);
			Line2D right = new Line2D.Double(widthPixels, 0, widthPixels, heightPixels);
			Line2D down = new Line2D.Double(0, heightPixels, widthPixels, heightPixels);
			Point2D leftIntersect = Methods.getSegmentIntersection(left, beamLine);
			Point2D upIntersect = Methods.getSegmentIntersection(up, beamLine);
			Point2D rightIntersect = Methods.getSegmentIntersection(right, beamLine);
			Point2D downIntersect = Methods.getSegmentIntersection(down, beamLine);
			if (leftIntersect != null)
				b.end = new Point3D((int) leftIntersect.getX(), (int) leftIntersect.getY(), b.z);
			if (upIntersect != null)
				b.end = new Point3D((int) upIntersect.getX(), (int) upIntersect.getY(), b.z);
			if (rightIntersect != null)
				b.end = new Point3D((int) rightIntersect.getX(), (int) rightIntersect.getY(), b.z);
			if (downIntersect != null)
				b.end = new Point3D((int) downIntersect.getX(), (int) downIntersect.getY(), b.z);
			sprayDebris(b);
			return;
		}

		// collision handling
		Point2D intersectionPoint = Methods.getSegmentIntersection(collisionLine, beamLine);
		if (intersectionPoint == null)
		{
			b.endType = 0;
			b.endAngle = angle;
			sprayDebris(b);
			return;
		}
		Point roundedIntersectionPoint = new Point((int) intersectionPoint.getX(), (int) intersectionPoint.getY());
		Beam b2 = null; // ...
		switch (collisionType)
		{
		case 0: // wall
			b.end.x = roundedIntersectionPoint.x;
			b.end.y = roundedIntersectionPoint.y;
			b.endType = 1;

			if (roundedIntersectionPoint.x == collidedWall.x * squareSize) // left
				b.endAngle = 0;
			if (roundedIntersectionPoint.y == collidedWall.y * squareSize) // up
				b.endAngle = Math.PI / 2;
			if (roundedIntersectionPoint.x == collidedWall.x * squareSize + squareSize) // right
				b.endAngle = Math.PI;
			if (roundedIntersectionPoint.y == collidedWall.y * squareSize + squareSize) // down
				b.endAngle = -Math.PI / 2;
			damageWall(collidedWall.x, collidedWall.y, b.damage + b.pushback, EP.damageType(b.elementNum));
			break;
		case 1: // Force Field
			b.end.x = roundedIntersectionPoint.x;
			b.end.y = roundedIntersectionPoint.y;
			b.endType = 1;

			// if the new beam is within the FF...
			if (recursive)
			{
				b2 = getReflectedBeam(b, collisionLine);

				Line2D diagonal1 = new Line2D.Double(collidedFF.p[0], collidedFF.p[2]);
				Line2D diagonal2 = new Line2D.Double(collidedFF.p[1], collidedFF.p[3]);
				if (b2 != null)
				{
					Line2D beam2Line = new Line2D.Double(b2.start.x, b2.start.y, b2.end.x, b2.end.y);
					if (diagonal1.intersectsLine(beam2Line) || diagonal2.intersectsLine(beam2Line)) // only happens (i think) when you create a beam inside a force field
					{
						; // is bad. TODO make this code slightly shorter?
					}
					else
					{
						beams.add(b2);
						moveBeam(b2, true, deltaTime); // Recursion!!!
						// sound effect (reflection electric sound)
						if (!collidedFF.sounds.get(0).active)
							collidedFF.sounds.get(0).loop();
						else
							collidedFF.sounds.get(0).justActivated = true;
					}
				}
			}
			break;
		case 2: // Arc Force Field
			b.end.x = roundedIntersectionPoint.x;
			b.end.y = roundedIntersectionPoint.y;
			b.endType = 0;
			if (EP.damageType(b.elementNum) == 4) // electricity or energy
			{
				if (recursive)
				{
					b2 = getReflectedBeam(b, collisionLine);
					if (b2 != null) // BUGS MADE ME DO THIS
					{
						beams.add(b2);
						moveBeam(b2, true, deltaTime); // Recursion!!!
					}
				}
				damageArcForceField(collidedAFF, deltaTime * (b.damage + b.pushback), roundedIntersectionPoint, EP.damageType(b.elementNum));
			}
			break;
		case 3: // Person
			b.end.x = roundedIntersectionPoint.x;
			b.end.y = roundedIntersectionPoint.y;
			b.endType = 0;
			// can't check every beam...only check 5% of them
			if (Math.random() < 0.05 && checkForEvasion(collidedPerson))
				b.theAbility.evadedBy(collidedPerson);
			else
			{
				// damaging the person
				hitPerson(collidedPerson, b.damage, b.pushback, angle, b.elementNum, deltaTime);

				switch (b.elementNum)
				{
				case 0: // fire
				case 6: // energy
				case 8: // lava
					// sound effect (scorched flesh)
					if (!collidedPerson.sounds.get(0).active)
						collidedPerson.sounds.get(0).loop();
					else
						collidedPerson.sounds.get(0).justActivated = true;
					break;
				default:
					break;

				}
			}
			break;
		case 4: // Ball
			b.end.x = roundedIntersectionPoint.x;
			b.end.y = roundedIntersectionPoint.y;
			b.endType = 0;

			collidedBall.mass -= 2 * b.damage * deltaTime * collidedBall.timeEffect; // TODO what the shit? Damaging the ball's mass? Whaaa?
			// I'm not sure what I did here with the angles but it looks OK
			if (Math.random() < 0.5)
				ballDebris(collidedBall, "beam hit", collidedBall.angle());
			if (collidedBall.mass < 0) // additional debris for destruction
				ballDebris(collidedBall, "shatter", collidedBall.angle());
			break;
		case 5: // Vine
			b.end.x = roundedIntersectionPoint.x;
			b.end.y = roundedIntersectionPoint.y;
			b.endType = 1;
			boolean sideOfVine = (b.x - collidedVine.start.x) * Math.sin(b.angle()) > (b.y - collidedVine.y) * Math.cos(b.angle());
			b.endAngle = collidedVine.rotation + (sideOfVine ? TAU / 4 : -TAU / 4);

			collidedVine.life -= b.damage;
			// TODO vine plant debris
			break;
		case 6: // Portal
			b.end.x = roundedIntersectionPoint.x;
			b.end.y = roundedIntersectionPoint.y;
			b.endType = 1;
			boolean sideOfPortal = (b.x - collidedPortal.start.x) * Math.sin(b.angle()) > (b.y - collidedPortal.y) * Math.cos(b.angle());
			b.endAngle = collidedPortal.angle + (sideOfPortal ? TAU / 4 : -TAU / 4);

			if (recursive)
			{
				b2 = getPortaledBeam(b, collidedPortal, roundedIntersectionPoint);

				if (b2 != null)
				{
					beams.add(b2);
					moveBeam(b2, true, deltaTime); // Recursion!!!
				}
			}
			break;
		default:
			MAIN.errorMessage("Dragon and Defiant, sitting in a tree, K-I-S-S-I-S-S-I-P-P-I");
			break;
		}

		if (b.endType != 1)
			b.endAngle = angle;
		sprayDebris(b);
	}

	void sprayDebris(Beam b)
	{
		double angle = Math.atan2(b.end.y - b.start.y, b.end.x - b.start.x);
		// Spray debris under beam if needed
		if (Math.random() < 0.12) // 12% chance
			switch (b.elementNum)
			{
			case 1: // water
			case 7: // acid
			case 8: // lava
			case 9: // blood
				double distance = Math.sqrt(Methods.DistancePow2(b.start, b.end));
				int amount = (int) (distance / 200);
				for (int i = 0; i < amount; i++)
				{
					double d = Math.random() * distance;
					debris.add(new Debris(b.start.x + d * Math.cos(angle) + d / 10 * Math.sin(angle) * (Math.random() * 2 - 1),
							b.start.y + d * Math.sin(angle) + d / 10 * Math.cos(angle) * (Math.random() * 2 - 1), b.z, Math.random() * Math.PI * 2, b.elementNum, 0));
				}
				break;
			default:
				break;
			}
	}

	Beam getReflectedBeam(Beam b, Line2D collisionLine)
	{
		double angle = Math.atan2(b.end.y - b.start.y, b.end.x - b.start.x);
		double lineAngle = Math.atan2(collisionLine.getY2() - collisionLine.getY1(), collisionLine.getX2() - collisionLine.getX1());
		b.endAngle = lineAngle + Math.PI / 2;
		double newBeamAngle = 2 * lineAngle - angle; // math
		// reflection beam
		final double startDistance = 15; // if this is lower than ~15, there will be flickering in some reflections, but if this is higher than ~15 there will be a noticeable starting distance, especially with tiny beams.
		// possible TODO to solve that problem : multiply this number by 90-the angle between the laser and the FF's hit side.
		double beamLength = Math.sqrt(Math.pow(b.end.x - b.start.x, 2) + Math.pow(b.end.y - b.start.y, 2)); // Should work
		double newRange = b.range - beamLength;
		if (newRange < 0) // because bugs
			return null;
		Beam b2 = new Beam(b.creator, b.theAbility, new Point3D((int) (b.end.x + startDistance * Math.cos(newBeamAngle)), (int) (b.end.y + startDistance * Math.sin(newBeamAngle)), b.end.z - 0.01),
				new Point3D((int) (b.end.x + newRange * Math.cos(newBeamAngle)), (int) (b.end.y + newRange * Math.sin(newBeamAngle)), b.end.z - 0.01), b.elementNum, b.damage, b.pushback, newRange);
		b2.frameNum = b.frameNum;
		b2.isChild = true;
		return b2;
	}

	Beam getPortaledBeam(Beam b, Portal p, Point intersection)
	{
		double beamLength = Math.sqrt(Math.pow(b.end.x - b.start.x, 2) + Math.pow(b.end.y - b.start.y, 2)); // Should work
		double newRange = b.range - beamLength;
		if (newRange < 0) // because bugs
			return null;
		double newBeamAngle = b.angle() + p.partner.angle - p.angle;
		final double startDistance = 10; // Extra distance from partner portal
		double angleRelativeToPartner = Math.atan2(intersection.y - p.y, intersection.x - p.x) + p.partner.angle - p.angle;
		double distanceToPortalCenter = Math.sqrt(Methods.DistancePow2(p.x, p.y, b.end.x, b.end.y));
		Beam b2 = new Beam(b.creator, b.theAbility,
				new Point3D((int) (p.partner.x + distanceToPortalCenter * Math.cos(angleRelativeToPartner) + startDistance * Math.cos(newBeamAngle)),
						(int) (p.partner.y + distanceToPortalCenter * Math.sin(angleRelativeToPartner) + startDistance * Math.sin(newBeamAngle)), b.end.z + p.partner.z - p.z - 0.01),
				new Point3D((int) (p.partner.x + distanceToPortalCenter * Math.cos(angleRelativeToPartner) + (startDistance + newRange) * Math.cos(newBeamAngle)),
						(int) (p.partner.y + distanceToPortalCenter * Math.sin(angleRelativeToPartner) + (startDistance + newRange) * Math.sin(newBeamAngle)), b.end.z + p.partner.z - p.z - 0.01),
				b.elementNum, b.damage, b.pushback, newRange);
		b2.frameNum = b.frameNum;
		b2.isChild = true;
		return b2;
	}

	public void ballDebris(Ball b, String type, double angle)
	{
		double velocityModifier = 1;
		int amount = 1;
		switch (b.elementNum)
		{
		case 1: // water
		case 5: // ice
		case 7: // acid
		case 8: // lava
		case 3: // electricity
			velocityModifier = 0;
			amount = 2;
			break;
		case 0: // fire
		case 2: // wind
		case 9: // flesh
		case 4: // metal
		case 6: // energy
		case 10: // earth
		case 11: // plant
		default:
			velocityModifier = 1;
			break;
		}
		double randomAngle = -1;
		double randomDistance = 0;
		double distanceModifier = 200;
		// "angle" is not always necessary
		switch (type)
		{
		case "wall":
			for (int k = 0; k < 3 * amount; k++)
			{
				randomAngle = Math.random() * TAU;
				randomDistance = distanceModifier * Math.random() * (1 - velocityModifier);
				// 3 pieces of debris on every side, spread angle is 20*3 degrees (180/9) on every side
				debris.add(new Debris(b.x + randomDistance * Math.cos(randomAngle), b.y + randomDistance * Math.sin(randomAngle), b.z, angle - 0.5 * Math.PI + k * Math.PI / 9, b.elementNum,
						b.velocity() * 0.9 * velocityModifier, b.timeEffect));
				debris.add(new Debris(b.x + randomDistance * Math.cos(randomAngle), b.y + randomDistance * Math.sin(randomAngle), b.z, angle + 0.5 * Math.PI - k * Math.PI / 9, b.elementNum,
						b.velocity() * 0.9 * velocityModifier, b.timeEffect));
			}
			playSound(EP.elementList[b.elementNum] + " Smash.wav", b.Point());
			break;
		case "shatter":
			for (int i = 0; i < 7 * amount; i++)
			{
				randomAngle = Math.random() * TAU;
				randomDistance = distanceModifier * Math.random() * (1 - velocityModifier);
				// I'm not sure what I did here with the angles but it looks OK
				debris.add(new Debris(b.x + randomDistance * Math.cos(randomAngle), b.y + randomDistance * Math.sin(randomAngle), b.z, angle + 4 + i * (4) / 6, b.elementNum, 500 * velocityModifier,
						b.timeEffect));
			}
			playSound(EP.elementList[b.elementNum] + " Smash.wav", b.Point());
			break;
		case "arc force field":
			for (int i = 0; i < 3 * amount; i++)
			{
				randomAngle = Math.random() * TAU;
				randomDistance = distanceModifier * Math.random() * (1 - velocityModifier);
				// 3 pieces of debris on every side, spread angle is 20*3 degrees (180/9) on every side
				debris.add(new Debris(b.x + randomDistance * Math.cos(randomAngle), b.y + randomDistance * Math.sin(randomAngle), b.z, angle + 0.5 * Math.PI + i * Math.PI / 9, b.elementNum,
						b.velocity() * 0.9 * velocityModifier, b.timeEffect));
				debris.add(new Debris(b.x + randomDistance * Math.cos(randomAngle), b.y + randomDistance * Math.sin(randomAngle), b.z, angle - 0.5 * Math.PI - i * Math.PI / 9, b.elementNum,
						b.velocity() * 0.9 * velocityModifier, b.timeEffect));
			}
			playSound(EP.elementList[b.elementNum] + " Smash.wav", b.Point());
			break;
		case "punch":
			// effects
			for (int k = 0; k < 7 * amount; k++) // epicness
			{
				randomAngle = Math.random() * TAU;
				randomDistance = distanceModifier * Math.random() * (1 - velocityModifier);
				debris.add(new Debris(b.x + randomDistance * Math.cos(randomAngle), b.y + randomDistance * Math.sin(randomAngle), b.z, angle - 3 * 0.3 + k * 0.3, b.elementNum, 600 * velocityModifier,
						b.timeEffect));
			}
			playSound(EP.elementList[b.elementNum] + " Smash.wav", b.Point());
			break;
		case "beam hit":
			randomAngle = Math.random() * TAU;
			randomDistance = distanceModifier * Math.random() * (1 - velocityModifier);
			debris.add(new Debris(b.x + randomDistance * Math.cos(randomAngle), b.y + randomDistance * Math.sin(randomAngle), b.z, Math.random() * 2 * Math.PI, b.elementNum, 500 * velocityModifier,
					b.timeEffect));
			break;
		default:
			MAIN.errorMessage("I'm sorry, I couldn't find any results for \"debris\". Perhaps you meant \"Deborah Peters\"?");
			break;
		}
	}

	public void damageWall(int i, int j, double damage, int damageType)
	{
		if (wallTypes[i][j] == -2)
			return;
		// fire can't hurt lava, acid can't hurt acid, electricity can't hurt energy, etc.
		if (damageType > 1 && EP.damageType(getWallElement(wallTypes[i][j])) == damageType)
			return;
		// TODO elemental bonuses against some walls.
		// note: because there are so many walls, destroying the wall occurs here and not in a frame check like other objects.
		final int wallArmor = 10;
		if (damage - wallArmor < 1)
		{
			return;
		}
		wallHealths[i][j] -= (int) (damage - wallArmor);
		// commented out:
		// if (showDamageNumbers)
		// uitexts.add(new UIText(i * squareSize + squareSize / 2 - 10, j * squareSize + squareSize / 2 - 10, "" + (int) (damage - wallArmor), 5));
		if (wallHealths[i][j] <= 0)
			destroyWall(i, j);
		if (damage > 30 && damageType == 0) // hits additional walls if high and blunt damage
		{
			damageConnectedWalls(i, j, damage, damageType);
		}
		connectWall(i, j); // update cracks
	}

	public void damageFurniture(Furniture f, double damage, @SuppressWarnings("unused") int damageType)
	{
		if (damage - f.armor < 1)
			return;
		f.damage((int) (damage - f.armor));
		if (showDamageNumbers)
			uitexts.add(new UIText((int) f.x - 10, (int) f.y - 10, "" + (int) damage, 5));
		for (int k = 0; k < f.debrisToCreate; k++)
		{
			for (int i = 0; i < 2; i++)
				debris.add(new FurnitureDebris(f.x, f.y, f.z, Math.PI * 2 * Math.random(), f.originalImage, 150 + (int) (Math.random() * 150)));
		}
		f.debrisToCreate = 0;
	}

	boolean[][] connectedWalls;

	void damageConnectedWalls(int x, int y, double damage, int damageType)
	{
		connectedWalls = new boolean[width][height];

		// damages other walls by damage * 1 / (distance-1)^2
		int extra = (int) (((damage - 1) / 11)); // extra number of grid squares from this wall that can possibly be damaged

		// Connect walls that are connected
		List<Point> iteratedWalls = new ArrayList<Point>();
		iteratedWalls.add(new Point(x, y));
		connectedWalls[x][y] = true;
		List<Point> nextWalls = new ArrayList<Point>();
		int step = 1;
		while (step <= extra)
		{
			for (int k = 0; k < iteratedWalls.size(); k++)
			{
				Point w = iteratedWalls.get(k);
				for (int i = -1; i <= 1; i++)
					for (int j = -1; j <= 1; j++)
					{
						if (w.x + i < 0 || w.y + j < 0 || w.x + i >= width || w.y + j >= height)
							continue;
						if (connectedWalls[w.x + i][w.y + j])
							continue;
						if (wallTypes[w.x + i][w.y + j] >= 0)
						{
							connectedWalls[w.x + i][w.y + j] = true;
							nextWalls.add(new Point(w.x + i, w.y + j));
						}
					}
			}
			iteratedWalls.clear();
			iteratedWalls.addAll(nextWalls);
			nextWalls.clear();
			step++;
		}

		for (int xx = Math.max(x - extra, 0); xx < x + extra && xx < width; xx++)
			for (int yy = Math.max(y - extra, 0); yy < y + extra && yy < height; yy++)
				if (connectedWalls[xx][yy])
					if (wallHealths[xx][yy] > 0 && (xx != x || yy != y))
						nonRecursiveDamageWall(xx, yy, damage / Math.sqrt(Methods.DistancePow2(x, y, xx, yy)), damageType);
	}

	/**
	 * Assumes wall exists in there and does not update cracks
	 * 
	 * @param x
	 * @param y
	 * @param damage
	 */
	void quickDamageWall(int i, int j, double damage)
	{
		if (wallTypes[i][j] == -2)
			return;
		final int wallArmor = 10;
		if (damage - wallArmor < 1)
			return;
		wallHealths[i][j] -= (int) (damage - wallArmor);
		if (wallHealths[i][j] <= 0)
			destroyWall(i, j);
	}

	public void nonRecursiveDamageWall(int i, int j, double damage, int damageType)
	{
		if (wallTypes[i][j] == -2)
			return;
		if (damageType > 1 && EP.damageType(getWallElement(wallTypes[i][j])) == damageType)
			return;
		final int wallArmor = 10;
		if (damage - wallArmor < 1)
			return;
		wallHealths[i][j] -= (int) (damage - wallArmor);
		// commented out:
		// if (showDamageNumbers)
		// uitexts.add(new UIText(i * squareSize + squareSize / 2 - 10, j * squareSize + squareSize / 2 - 10, "" + (int) (damage - wallArmor), 5));
		if (wallHealths[i][j] <= 0)
			destroyWall(i, j);
		connectWall(i, j); // update cracks
	}

	public void damageForceField(ForceField ff, double damage, Point point)
	{
		damage -= ff.armor;
		if (damage > 0)
		{
			ff.life -= damage;

			// TODO some visual effect?
			if (showDamageNumbers)
				uitexts.add(new UIText(point.x - 10, point.y - 10, "" + (int) damage, 3));
		}
	}

	public void damageArcForceField(ArcForceField aff, double damage, Point point, int damageType)
	{
		if (damageType > 1 && aff.damageType() == damageType) // resistance
			damage *= 0.5;
		damage -= aff.armor;
		if (damage <= 0)
			return;
		double prevLife = aff.life;
		aff.life -= damage;

		aff.extraLife -= damage; // damaging shields while they're being built deals them more damage
		if (aff.extraLife < 0)
			aff.extraLife = 0;

		if ((prevLife >= 15 && aff.life < 15) || (prevLife >= 50 && aff.life < 50) || (prevLife >= 75 && aff.life < 75)) // TODO make it happen for every layer (more than once if two layers are broken
			// at once), and also make it more random?
			shieldDebris(aff, "shield layer removed");

		if (showDamageNumbers)
		{
			if (showDamageNumbers)
				aff.waitingDamage += damage;
			if (aff.timeBetweenDamageTexts > 0.5)
			{
				if (showDamageNumbers)
				{
					if (aff.waitingDamage > 1)
					{
						uitexts.add(new UIText(point.x - 10, point.y - 10, "" + (int) aff.waitingDamage, 3));
						aff.waitingDamage -= (int) aff.waitingDamage;
					}
				}
				aff.timeBetweenDamageTexts = 0;
			}
		}
	}

	public void createExplosion(double x, double y, double z, double radius, double damage, double pushback, int element)
	{
		// NOTE!!!!!!!
		// The damage and pushback of explosions is calculated like this:
		// dmg = damage*(radius - distance)/radius
		// where "distance" = distance between explosion origin point (x, y) and victim's center.
		// This type of damage calculation makes it LINEAR.
		double damageToBuildings = (damage + pushback) * 0.8;

		// type -1 = regular explosion
		double explosionHeight = radius * 2 / 100;
		double timeLeft = 0.8;
		Explosion explosion = new Explosion(x, y, z, timeLeft, element);
		explosion.createSubExplosion(this, (int) x, (int) y, 2);
		playSound("Explosion_" + (int) (1 + Math.random() * 5) + ".wav", new Point((int) x, (int) y));
		explosions.add(explosion);
		// Adding the visual effect
		for (Person p : people)
			if (p.z < z + explosionHeight / 2 && p.highestPoint() > z - explosionHeight / 2)
				if (Methods.DistancePow2(p.x, p.y, x, y) < radius * radius)
				{
					double distance = Math.sqrt(Methods.DistancePow2(p.x, p.y, x, y));
					double damage2 = damage * (radius - distance) / radius;
					if (checkForEvasion(p))
						damage2 = 0;
					for (Ability a : p.abilities)
						if (a instanceof Explosion_Resistance && a.on)
							damage2 = 0; // still apply pushback
					double pushbackDealt = pushback;
					if ((int) p.x == (int) x && (int) p.y == (int) y) // explosions don't push people in the center
						pushbackDealt = 0;
					hitPerson(p, damage2, pushbackDealt * (radius - distance) / radius, Math.atan2(p.y - y, p.x - x), element == -1 ? 0 : element);
				}
		for (ForceField ff : FFs)
			if (ff.z < z + explosionHeight / 2 && ff.z + ff.height > z - explosionHeight / 2)
			{
				boolean withinRange = false;
				for (Point p : ff.p)
					if (Methods.DistancePow2(p.x, p.y, x, y) < radius * radius)
						withinRange = true;
				if (withinRange)
					damageForceField(ff, damageToBuildings * (radius - Math.sqrt(Methods.DistancePow2(ff.x, ff.y, x, y))) / radius, new Point((int) ff.x, (int) ff.y));
			}
		for (Furniture f : furniture)
			if (f.z < z + explosionHeight / 2 && f.z + f.height > z - explosionHeight / 2)
			{
				boolean withinRange = false;
				for (Point p : f.getPoints())
					if (Methods.DistancePow2(p.x, p.y, x, y) < radius * radius)
						withinRange = true;
				if (withinRange)
					damageFurniture(f, damageToBuildings * (radius - Math.sqrt(Methods.DistancePow2(f.x, f.y, x, y))) / radius, element == -1 ? 0 : EP.damageType(element));
			}
		for (ArcForceField aff : AFFs)
			if (aff.z < z + explosionHeight / 2 && aff.z + aff.height > z - explosionHeight / 2)
			{
				List<Point2D> affCorners = new ArrayList<Point2D>();
				affCorners.add(new Point2D.Double(aff.x + aff.minRadius * Math.cos(aff.rotation - aff.arc / 2), aff.y + aff.minRadius * Math.sin(aff.rotation - aff.arc / 2)));
				affCorners.add(new Point2D.Double(aff.x + aff.maxRadius * Math.cos(aff.rotation - aff.arc / 2), aff.y + aff.maxRadius * Math.sin(aff.rotation - aff.arc / 2)));
				affCorners.add(new Point2D.Double(aff.x + aff.minRadius * Math.cos(aff.rotation + aff.arc / 2), aff.y + aff.minRadius * Math.sin(aff.rotation + aff.arc / 2)));
				affCorners.add(new Point2D.Double(aff.x + aff.maxRadius * Math.cos(aff.rotation + aff.arc / 2), aff.y + aff.maxRadius * Math.sin(aff.rotation + aff.arc / 2)));
				boolean withinRange = false;
				for (Point2D p : affCorners)
					if (Methods.DistancePow2(p.getX(), p.getY(), x, y) < radius * radius)
						withinRange = true;
				if (withinRange)
				{
					Point affMiddle = new Point((int) (aff.x + (aff.minRadius + aff.maxRadius) / 2 * Math.cos(aff.rotation)),
							(int) (aff.y + (aff.minRadius + aff.maxRadius) / 2 * Math.sin(aff.rotation)));
					damageArcForceField(aff, damageToBuildings * (radius - Math.sqrt(Methods.DistancePow2(affMiddle.x, affMiddle.y, x, y))) / radius, affMiddle,
							element == -1 ? 0 : EP.damageType(element));
				}
			}
		if (z - explosionHeight / 2 < 1)
			for (int gridX = (int) (x - radius) / squareSize; gridX <= (int) (x + radius) / squareSize; gridX++)
				for (int gridY = (int) (y - radius) / squareSize; gridY <= (int) (y + radius) / squareSize; gridY++)
					if (gridX > 0 && gridY > 0 && gridX < width && gridY < height) // to avoid checking beyond array size
						if (wallHealths[gridX][gridY] > 0
								&& Methods.DistancePow2(x, y, gridX * squareSize + squareSize / 2, gridY * squareSize + squareSize / 2) < Math.pow(radius - squareSize / 2, 2))
							damageWall(gridX, gridY,
									damageToBuildings * (radius - Math.sqrt(Methods.DistancePow2(gridX * squareSize + squareSize / 2, gridY * squareSize + squareSize / 2, x, y))) / radius,
									element == -1 ? 0 : EP.damageType(element));

	}

	public void shieldDebris(ArcForceField aff, String type)
	{
		switch (type)
		{
		case "shield layer removed":
			for (int i = 0; i < 3; i++)
				// 86 is avg of minradius and maxradius
				debris.add(new Debris((int) (aff.x + 86 * Math.cos(aff.rotation)), (int) (aff.y + 86 * Math.sin(aff.rotation)), aff.z, aff.rotation + 0.5 * Math.PI + 0.3 * i, aff.elementNum, 150));
			break;
		case "deactivate":
			for (int i = 0; i < 3; i++)
			{
				// 86 is avg of minradius and maxradius
				debris.add(new Debris((int) (aff.x + 86 * Math.cos(aff.rotation)), (int) (aff.y + 86 * Math.sin(aff.rotation)), aff.z, aff.rotation + 0.5 * Math.PI + 0.3 * i, aff.elementNum, 200));
				debris.add(new Debris((int) (aff.x + 86 * Math.cos(aff.rotation)), (int) (aff.y + 86 * Math.sin(aff.rotation)), aff.z, aff.rotation + 0.5 * Math.PI + 0.3 * i, aff.elementNum, 200));
			}
			break;
		case "bubble":
			for (int i = 0; i < 7; i++)
			{
				double angle = aff.rotation + i * TAU / 7 + Math.random();
				debris.add(new Debris((int) (aff.x + aff.maxRadius * Math.cos(angle)), (int) (aff.y + aff.maxRadius * Math.sin(angle)), aff.z, angle, 13, 150));
			}
			break;
		default:
			MAIN.errorMessage("\"Shit's wrecked!\" Yamana shouts. He points at the wrecked shit.");
			break;
		}
	}

	public void sprayDropDebris(SprayDrop sd)
	{
		switch (sd.elementNum)
		{
		case 0: // fire
		case 1: // water
		case 5: // ice
		case 7: // acid
		case 8: // lava
		case 9: // flesh
			debris.add(new Debris(sd.x, sd.y, sd.z, sd.angle(), sd.elementNum, 0));
			break;
		case 2: // wind
		case 3: // electricity
		case 4: // metal
		case 6: // energy
		case 10: // earth
		case 11: // plant
			break;
		default:
			MAIN.errorMessage("...");
		}
	}

	public void otherDebris(double x, double y, int elementNum, String type, int frameNum)
	{
		double velocityModifier = 1;
		switch (elementNum)
		{
		case 1: // water
		case 5: // ice
		case 7: // acid
		case 8: // lava
		case 3: // electricity
		case 14: // blood
			velocityModifier = 0;
			break;
		case 0: // fire
		case 2: // wind
		case 9: // flesh
		case 4: // metal
		case 6: // energy
		case 10: // earth
		case 11: // plant
		default:
			velocityModifier = 1;
			break;
		}
		switch (type)
		{
		case "pool heal":
		case "wall heal":
			if (frameNum % 10 == 0)
				for (double i = Math.random(); i < 3; i++)
				{
					double angle = Math.random() * Math.PI * 2;
					double distance = Math.random() * 100 + 50;
					debris.add(new Debris(x + distance * Math.cos(angle), y + distance * Math.sin(angle), 0, i + Math.random(), elementNum, 300 * velocityModifier));
				}
			break;
		case "destroy":
			for (int i = 0; i < 5; i++)
				debris.add(new Debris(x * squareSize + 0.5 * squareSize, y * squareSize + 0.5 * squareSize, 0, Math.PI * 2 / 5 * i, elementNum, 200));
			break;
		case "trail":
			for (int i = 0; i < 3; i++)
			{
				double randomDirection = TAU * Math.random();
				double randomDistance = 20 + Math.random() * 80;
				debris.add(
						new Debris(x + randomDistance * Math.cos(randomDirection), y + randomDistance * Math.sin(randomDirection), 0, Math.PI * 2 * Math.random(), elementNum, 200 * velocityModifier));
			}
			break;
		default:
			MAIN.errorMessage("Error message 7: BEBHMAXBRI0903 T");
			break;
		}
	}

	public int getWallDebrisType(int wallType)
	{
		switch (wallType)
		{
		case -2:
			return -1;
		case 12: // cement
			return 15;
		default:
			return wallType;
		}
	}

	public int getWallElement(int wallType)
	{
		switch (wallType)
		{
		case -2: // edges
			return 10;
		case 12: // cement
			return 10;
		default:
			return wallType;
		}
	}

	public int getPoolDebrisType(int poolType)
	{
		switch (poolType)
		{
		case -2:
			return -1;
		default:
			return poolType;
		}
	}

	/**
	 * 
	 * @param p
	 *            - the person being hit
	 * @param damage
	 *            - amount of damage
	 * @param pushback
	 *            - amount of pushback
	 * @param angle
	 *            - angle in which the person might be pushed
	 * @param elementNum
	 *            - element of the damage. Not damage type!
	 * @param percentageOfTheDamage
	 *            - out of a second of damage. This is usually equal to deltaTime or 1.
	 */
	public void hitPerson(Person p, double damage, double pushback, double angle, int elementNum, double percentageOfTheDamage)
	{
		// percentages are multiplied by time stretch effects
		percentageOfTheDamage *= p.timeEffect;
		damage *= percentageOfTheDamage;
		pushback *= percentageOfTheDamage;
		if (damage > 0.01)
		{
			// damage is multiplied by 0.9 to 1.1
			damage *= 0.9 + Math.random() * 0.2;

			// pushback takes into account pushback resistance
			pushback -= pushback * p.pushbackResistance;
			
			int damageType = EP.damageType(elementNum);

			damage = p.damageAfterHittingArmor(damage, damageType, percentageOfTheDamage);
			if (p.ghostMode && damageType != -2) // ghost wall-clipping? 
				if (damageType == 2 || damageType == 4)
				{
					// burn or shock damage deal more while ethereal
					damage *= 3;
					pushback = 0;
				}
				else
				{
					damage = 0;
					pushback = 0;
				}
			// Elemental resistance
			for (Effect e : p.effects)
				if (e instanceof E_Resistant)
					if (EP.damageType(((E_Resistant) e).element) == damageType)
					{
						if (e.strength >= 5)
							damage = 0;
						else
							damage *= Math.pow(0.75, e.strength); // maybe change it to 1-0.15*e.strength ?
					}

			double randomNumber = Math.random(); // for elemental effect checks

			// Elemental effects!
			if (percentageOfTheDamage <= 0.02) // as it does whenever it is not 1, most likely. still, TODO make this good code
				randomNumber *= 4.1; // This is roughly the right amount to keep it a 15% chance per second
			double[] dmgpush = trySpecialEffectReturnDamageAndPushback(p, elementNum, damage, pushback, randomNumber);
			damage = dmgpush[0];
			pushback = dmgpush[1];

			// Grunt sound, if damage is bad enough
			if (!p.dead)
				if (damage >= 0.12 * p.maxLife)
					p.sounds.get(2 + (int) (Math.random() * 5)).play(); // grunt

			// dealing the actual damage!
			p.damage(damage);
			// whoa! That was so awesome.

			if (showDamageNumbers)
				p.waitingDamage += damage;
			if (p.timeBetweenDamageTexts > 0.5)
			{
				if (showDamageNumbers)
				{
					if (p.waitingDamage > 1)
					{
						p.uitexts.add(new UIText(-10, 0 - p.radius / 2 - 10, "" + (int) p.waitingDamage, 1));
						p.waitingDamage -= (int) p.waitingDamage;
					}
				}
				p.timeBetweenDamageTexts = 0;
			}
			else if (percentageOfTheDamage == 1)
				if (!p.uitexts.isEmpty())
				{
					UIText lastDamageText = p.uitexts.get(p.uitexts.size() - 1);
					if (lastDamageText.type == 1) // TODO use enums!
						lastDamageText.addAmount((int) p.waitingDamage);
					p.waitingDamage -= (int) p.waitingDamage;
				}
		}
		// PUSHBACK
		double velocityPush = pushback * 3000 / (p.mass + 10 * p.STRENGTH); // the 3000 is subject to change
		p.xVel += velocityPush * Math.cos(angle);
		p.yVel += velocityPush * Math.sin(angle);

	}

	public double[] trySpecialEffectReturnDamageAndPushback(Person p, int elementNum, double damage, double pushback, double randomNumber)
	{
		switch (elementNum)
		{
		case 0: // fire
		case 8: // lava
			// Burn
			if (randomNumber < 0.15) // 15% chance
				p.affect(new Burning(5, null), true);
			break;
		case 1: // water
			// Slip
			if (randomNumber < 0.15) // 15% chance
				p.slip(true);
			break;
		case 3: // electricity
			// Stun
			if (randomNumber < 0.15) // 15% chance
				;// TODO
			break;
		case 5: // ice
			// Freeze
			if (randomNumber < 0.15) // 15% chance
				;// TODO
			break;
		case 11: // plant
			// Tangle
			if (randomNumber < 0.40) // 40% chance
				p.affect(new Tangled(1, null), true);
			break;
		case 2: // wind
		case 4: // metal
			// +50% pushback
			if (randomNumber < 0.15) // 15% chance
				pushback *= 1.5;
			break;
		case 6: // energy
		case 7: // acid
		case 9: // flesh
			// +25% damage
			if (randomNumber < 0.15) // 15% chance
				damage *= 1.25;
			break;
		case 10: // earth
			// +25% damage or +50% pushback
			if (randomNumber < 0.15) // 15% chance
				if (randomNumber < 0.075)
					damage *= 1.25;
				else
					pushback *= 1.5;
			break;
		case -1: // blunt/"normal" damage
			break;
		case -2: // "spectral" damage
			break;
		default:
			MAIN.errorMessage("It's elementary! " + elementNum + "...?");
			break;
		}
		return new double[]
		{ damage, pushback };
	}

	/**
	 * Single-time damage.
	 * 
	 * @param p
	 *            - the person being hit
	 * @param damage
	 *            - amount of damage
	 * @param pushback
	 *            - amount of pushback
	 * @param angle
	 *            - angle in which the person might be pushed
	 * @param elementNum
	 *            - element of the damage. Not damage type!
	 */
	public void hitPerson(Person p, double damage, double pushback, double angle, int elementNum)
	{
		hitPerson(p, damage, pushback, angle, elementNum, 1);
	}

	/**
	 * 
	 * @param p
	 * @return true if p evaded successfully
	 */
	public boolean checkForEvasion(Person p)
	{
		if (Math.random() <= p.evasion) // EVASION
		{
			if (showDamageNumbers)
				if (p.uitexts.isEmpty() || p.uitexts.get(p.uitexts.size() - 1).transparency < 0.85 * 256)
				{
					p.uitexts.add(new UIText(-10, 0 - p.radius / 2 - 10, "Evaded!", 6));
				}
			p.timeBetweenDamageTexts = 0;
			return true;
		}
		return false;
	}

	public void updatePools()
	{
		checkedSquares = new boolean[width][height];
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
			{
				int element = poolTypes[i][j];
				if (element != -1)
				{
					// spreading health, updating corner transparencies
					healthSum = 0;
					poolNum = 0;
					recursivePoolCheck(i, j, element);
					recursivePoolCheck(i - 1, j, element);
					recursivePoolCheck(i, j - 1, element);
					recursivePoolCheck(i + 1, j, element);
					recursivePoolCheck(i, j + 1, element);
					recursivePoolCheck(i - 1, j - 1, element);
					recursivePoolCheck(i - 1, j + 1, element);
					recursivePoolCheck(i + 1, j - 1, element);
					recursivePoolCheck(i + 1, j + 1, element);
					poolHealths[i][j] = healthSum / poolNum;
					recursivePoolUpdate(i, j, element, healthSum / poolNum);
					recursivePoolUpdate(i - 1, j - 1, element, healthSum / poolNum);
					recursivePoolUpdate(i - 1, j + 1, element, healthSum / poolNum);
					recursivePoolUpdate(i + 1, j - 1, element, healthSum / poolNum);
					recursivePoolUpdate(i + 1, j + 1, element, healthSum / poolNum);

					// updating image, depending on the connections to pool corners
					boolean a = pCornerStyles[i][j][element] != -1; // top left
					boolean b = pCornerStyles[i + 1][j][element] != -1; // top right
					boolean c = pCornerStyles[i + 1][j + 1][element] != -1; // bottom right
					boolean d = pCornerStyles[i][j + 1][element] != -1; // bottom left

					if (!a && !b && !c && !d) // lone pool
						poolImages[i][j] = Resources.pool[element];

					else if (a && !b && !c && !d) // LU
						poolImages[i][j] = Resources.croppedPool[element][0];
					else if (!a && b && !c && !d) // RU
						poolImages[i][j] = Resources.croppedPool[element][1];
					else if (!a && !b && c && !d) // RB
						poolImages[i][j] = Resources.croppedPool[element][2];
					else if (!a && !b && !c && d) // LB
						poolImages[i][j] = Resources.croppedPool[element][3];

					else if (!a && b && !c && d) // RU + LB
						poolImages[i][j] = Resources.croppedPool[element][4];
					else if (a && !b && c && !d) // LU + RB
						poolImages[i][j] = Resources.croppedPool[element][5];

					else if (!a && b && c && !d) // R
						poolImages[i][j] = Resources.croppedPool[element][6];
					else if (!a && !b && c && d) // B
						poolImages[i][j] = Resources.croppedPool[element][7];
					else if (a && !b && !c && d) // L
						poolImages[i][j] = Resources.croppedPool[element][8];
					else if (a && b && !c && !d) // U
						poolImages[i][j] = Resources.croppedPool[element][9];

					else if (!a && b && c && d) // not LU
						poolImages[i][j] = Resources.croppedPool[element][10];
					else if (a && !b && c && d) // not RU
						poolImages[i][j] = Resources.croppedPool[element][11];
					else if (a && b && !c && d) // not RB
						poolImages[i][j] = Resources.croppedPool[element][12];
					else if (a && b && c && !d) // not LB
						poolImages[i][j] = Resources.croppedPool[element][13];

					else if (a && b && c && d) // all corners
						poolImages[i][j] = null;

				}
			}
	}

	private void recursivePoolCheck(int x, int y, int elementNum)
	{
		if (x < 0 || y < 0 || x >= width || y >= height)
			return;
		if (poolTypes[x][y] != elementNum || checkedSquares[x][y])
			return;
		healthSum += poolHealths[x][y];
		poolNum++;
		checkedSquares[x][y] = true;
		recursivePoolCheck(x - 1, y, elementNum);
		recursivePoolCheck(x, y - 1, elementNum);
		recursivePoolCheck(x + 1, y, elementNum);
		recursivePoolCheck(x, y + 1, elementNum);
		recursivePoolCheck(x - 1, y - 1, elementNum);
		recursivePoolCheck(x + 1, y - 1, elementNum);
		recursivePoolCheck(x + 1, y + 1, elementNum);
		recursivePoolCheck(x - 1, y + 1, elementNum);
	}

	private void recursivePoolUpdate(int x, int y, int elementNum, int newHealth)
	{
		if (x < 0 || y < 0 || x > width - 1 || y > height - 1)
			return;
		if (poolTypes[x][y] != elementNum || !checkedSquares[x][y])
			return;
		poolHealths[x][y] = newHealth;
		if (pCornerStyles[x][y][elementNum] != -1)
			pCornerTransparencies[x][y][elementNum] = newHealth;
		if (pCornerStyles[x + 1][y][elementNum] != -1)
			pCornerTransparencies[x + 1][y][elementNum] = newHealth;
		if (pCornerStyles[x][y + 1][elementNum] != -1)
			pCornerTransparencies[x][y + 1][elementNum] = newHealth;
		if (pCornerStyles[x + 1][y + 1][elementNum] != -1)
			pCornerTransparencies[x + 1][y + 1][elementNum] = newHealth;
		checkedSquares[x][y] = false;
		recursivePoolUpdate(x - 1, y, elementNum, newHealth);
		recursivePoolUpdate(x, y - 1, elementNum, newHealth);
		recursivePoolUpdate(x + 1, y, elementNum, newHealth);
		recursivePoolUpdate(x, y + 1, elementNum, newHealth);
		recursivePoolUpdate(x - 1, y - 1, elementNum, newHealth);
		recursivePoolUpdate(x + 1, y - 1, elementNum, newHealth);
		recursivePoolUpdate(x + 1, y + 1, elementNum, newHealth);
		recursivePoolUpdate(x - 1, y + 1, elementNum, newHealth);
	}

	public void playSound(String s, Point p)
	{
		SoundEffect sound = new SoundEffect(s);
		sound.setPosition(p);
		ongoingSounds.add(sound);
		sound.play();
	}

	public void draw(Graphics2D buffer, int cameraZed, final Rectangle bounds)
	{
		List<Drawable> drawableThings = new ArrayList<Drawable>();
		drawableThings.addAll(people);
		drawableThings.addAll(clouds);
		drawableThings.addAll(balls);
		drawableThings.addAll(FFs);
		drawableThings.addAll(debris);
		drawableThings.addAll(AFFs);
		drawableThings.addAll(beams);
		drawableThings.addAll(vines);
		drawableThings.addAll(sprayDrops);
		drawableThings.addAll(portals);
		drawableThings.addAll(furniture);
		Predicate<Drawable> outOfScreen = new Predicate<Drawable>()
		{
			public boolean test(Drawable arg0)
			{
				// debugging
				if (arg0 == null || arg0.image == null)
				{
					MAIN.errorMessage("ummm what? no image found for " + arg0 + " or maybe it's null");
					return true;
				}
				if (arg0.image.getWidth() == 1 && arg0.image.getHeight() == 1) // for stuff like Beam images
					return false;
				if (arg0.x + arg0.image.getWidth() / 2 < bounds.getMinX() || arg0.x - arg0.image.getWidth() / 2 > bounds.getMaxX() || arg0.y + arg0.image.getHeight() / 2 < bounds.getMinY()
						|| arg0.y - arg0.image.getHeight() / 2 > bounds.getMaxY())
					return true;
				return false;
			}
		};
		Comparator<Drawable> sortDrawablesbyHeight = new Comparator<Drawable>()
		{
			public int compare(Drawable d1, Drawable d2)
			{
				Double i1 = new Double(d1.highestPoint());
				Double i2 = new Double(d2.highestPoint());
				if (d1 instanceof Portal && d2.highestPoint() > d1.z)
					i1 = -1.0; // Portals are always drawn underneath other things in same Z
				if (d2 instanceof Portal && d1.highestPoint() > d2.z)
					i2 = -1.0; //
				return i1.compareTo(i2);
			}
		};
		drawableThings.removeIf(outOfScreen);
		Collections.sort(drawableThings, sortDrawablesbyHeight);

		// Clouds, people, balls, force fields, debris, arc force fields, beams, vines, drops
		drawDrawables(buffer, cameraZed, drawableThings, -1, 1);

		// Walls and wall corners
		drawWalls(buffer, bounds);

		// Clouds, people, balls, force fields, debris, arc force fields, beams, vines, drops
		drawDrawables(buffer, cameraZed, drawableThings, 1, Integer.MAX_VALUE);
	}

	public void drawFloor(Graphics2D buffer, final Rectangle bounds)
	{
		// Pools (and floors)
		for (int x = 0; x < width; x++)
			if (x * squareSize > bounds.getMinX() - squareSize && x * squareSize < bounds.getMaxX())
				for (int y = 0; y < height; y++)
					if (y * squareSize > bounds.getMinY() - squareSize && y * squareSize < bounds.getMaxY())
					{
						// floor
						if (floorTypes[x][y] != -1)
							buffer.drawImage(Resources.floor[floorTypes[x][y]], x * squareSize, y * squareSize, null);
						// pools
						if (poolTypes[x][y] != -1)
						{
							buffer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f + 0.009f * poolHealths[x][y]));
							buffer.drawImage(poolImages[x][y], x * squareSize, y * squareSize, null);
							buffer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
						}
						// pool corners
						for (int i = pCornerStyles[0][0].length - 1; i >= 0; i--)
							// decreasing order because I want earth walls to be bottomest and earth is one of the last elements
							if (pCornerStyles[x][y][i] != -1)
							{
								BufferedImage cornerImg = Resources.pCorner[i][getCornerStyle(pCornerStyles[x][y][i])][Environment.getCornerAngle(pCornerStyles[x][y][i])];
								buffer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f + 0.009f * pCornerTransparencies[x][y][i]));
								buffer.drawImage(cornerImg, x * squareSize - squareSize / 2, y * squareSize - squareSize / 2, null);
								buffer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
							}
					}
	}

	public void drawWalls(Graphics2D buffer, Rectangle bounds)
	{
		for (int x = 0; x < width; x++)
			if (x * squareSize > bounds.getMinX() - squareSize && x * squareSize < bounds.getMaxX())
				for (int y = 0; y < height; y++)
					if (y * squareSize > bounds.getMinY() - squareSize && y * squareSize < bounds.getMaxY())
					{
						// walls
						if (wallTypes[x][y] == -2)
						{
							buffer.setColor(new Color(165, 165, 165));
							buffer.fillRect(x * squareSize, y * squareSize, squareSize, squareSize);
						}
						else if (wallTypes[x][y] != -1)
						{
							buffer.drawImage(Resources.wall[wallTypes[x][y]], x * squareSize, y * squareSize, null);
							if (wallHealths[x][y] <= 25)
								buffer.drawImage(Resources.cracks[2][11], x * squareSize, y * squareSize, null);
							else if (wallHealths[x][y] <= 50)
								buffer.drawImage(Resources.cracks[1][11], x * squareSize, y * squareSize, null);
							else if (wallHealths[x][y] <= 75)
								buffer.drawImage(Resources.cracks[0][11], x * squareSize, y * squareSize, null);
						}
						// wall corners
						for (int i = wCornerStyles[0][0].length - 1; i >= 0; i--)
							if (wCornerStyles[x][y][i] != -1)
							{
								BufferedImage cornerImg = Resources.wCorner[i][getCornerStyle(wCornerStyles[x][y][i])][Environment.getCornerAngle(wCornerStyles[x][y][i])];
								buffer.drawImage(cornerImg, x * squareSize - squareSize / 2, y * squareSize - squareSize / 2, null);

								// cracks
								if (cornerCracks[x][y] != -1) // 0 = <75, 1 = <50, 2 = <25
								{
									cornerImg = Resources.cracks[cornerCracks[x][y]][wCornerStyles[x][y][i]];
									// no rotation
									buffer.drawImage(cornerImg, x * squareSize - squareSize / 2, y * squareSize - squareSize / 2, null);
								}
							}
					}
	}

	public void drawDrawables(Graphics2D buffer, int cameraZed, List<Drawable> drawableThings, double minZ, double maxZ)
	{
		for (Drawable d : drawableThings)
		{
			if (d.getClass().equals(Player.class) && ((Player) d).ghostMode) // very specific change, for ghost-players
			{
				if (d.z + 1 >= minZ && d.z + 1 < maxZ)
				{
					// Ghosts are drawn as if they were 1 z-unit above other things, so that the player can see the phasing effect
					d.draw(buffer, cameraZed);

					// copypasta code from down below... :(
					//
					Person p = (Person) d;
					if (devMode) // draws helpful things in the x,y of the object. (NOT Z AXIS)
					{
						// center
						buffer.setColor(Color.green);
						buffer.drawRect((int) (p.x - 1), (int) (p.y - 1), 3, 3);
						// hitbox
						buffer.drawRect((int) (p.x - 0.5 * p.radius), (int) (p.y - 0.5 * p.radius), p.radius, p.radius);
						// radius
						buffer.drawOval((int) (p.x - 0.5 * p.radius), (int) (p.y - 0.5 * p.radius), p.radius, p.radius);
						// target
						buffer.setColor(Color.red);
						buffer.drawOval(p.target.x - 10, p.target.y - 10, 20, 20);
						// id
						buffer.setColor(Color.red);
						buffer.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
						buffer.drawString("" + p.id, (int) p.x, (int) p.y + p.imgH / 2);
						// movement line
						buffer.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
						buffer.setColor(Color.red);
						buffer.drawLine((int) (p.x), (int) (p.y), (int) (p.x + p.strengthOfAttemptedMovement * 100 * Math.cos(p.directionOfAttemptedMovement)),
								(int) (p.y + p.strengthOfAttemptedMovement * 100 * Math.sin(p.directionOfAttemptedMovement)));
					}
					//
				}
			}
			else if (d.z >= minZ && d.z < maxZ) // what will basically almost always happen for everything
			{
				// important drawings:
				d.drawShadow(buffer, shadowX, shadowY);
				d.draw(buffer, cameraZed);
				if (d instanceof Beam)
				{
					Beam b = (Beam) d;
					b.drawTopEffects(buffer, cameraZed); // TODO why not include this in draw()?
				}
				// dev-mode debugging unimportant drawings:
				if (devMode) // draws helpful things in the x,y of the object. (NOT Z AXIS)
				{
					if (d instanceof Portal)
					{
						Portal p = (Portal) d;
						if (p.partner != null)
						{
							buffer.setStroke(new BasicStroke(2));
							buffer.setColor(Color.red);
							buffer.drawLine((int) p.start.x, (int) p.start.y, (int) p.partner.start.x, (int) p.partner.start.y);
							buffer.setColor(Color.orange);
							buffer.drawLine((int) p.end.x, (int) p.end.y, (int) p.partner.end.x, (int) p.partner.end.y);
						}
					}
					if (d instanceof Vine)
					{
						Vine v = (Vine) d;
						buffer.setStroke(new BasicStroke(2));
						buffer.setColor(Color.red);
						buffer.drawOval(v.start.x - 3, v.start.y - 3, 7, 7);
						buffer.drawOval(v.end.x - 3, v.end.y - 3, 7, 7);
						buffer.drawLine(v.start.x, v.start.y, v.end.x, v.end.y);
						if (v.state == 0)
						{
							buffer.setColor(Color.white);
							buffer.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
							buffer.drawOval(v.end.x - Vine.grabblingRange, v.end.y - Vine.grabblingRange, 2 * Vine.grabblingRange, 2 * Vine.grabblingRange);
						}
						if (v.state == 1)
						{
							buffer.setColor(Color.blue);
							buffer.setStroke(new BasicStroke(2));
							buffer.drawLine((int) (v.end.x + v.deltaLength * Math.cos(v.rotation)), (int) (v.end.y + v.deltaLength * Math.sin(v.rotation)), v.end.x, v.end.y);
						}
					}
					if (d instanceof Beam)
					{
						Beam b = (Beam) d;
						buffer.setColor(Color.red);
						buffer.drawOval(b.start.x - 3, b.start.y - 3, 7, 7);
						buffer.drawOval(b.end.x - 3, b.end.y - 3, 7, 7);
						buffer.drawLine(b.start.x, b.start.y, b.end.x, b.end.y);
					}
					if (d instanceof Ball)
					{
						Ball b = (Ball) d;
						buffer.setColor(Color.red);
						buffer.drawRect((int) (b.x - 1), (int) (b.y - 1), 3, 3);
						buffer.setColor(Color.green);
						buffer.drawOval((int) (b.x - b.radius), (int) (b.y - b.radius), (int) (2 * b.radius), (int) (2 * b.radius));
						buffer.setColor(Color.red);
						buffer.drawLine((int) (b.x), (int) (b.y), (int) (b.x + b.xVel * 0.1), (int) (b.y + b.yVel * 0.1));
					}
					if (d instanceof SprayDrop)
					{
						SprayDrop sd = (SprayDrop) d;
						buffer.setColor(Color.red);
						buffer.drawRect((int) (sd.x - 1), (int) (sd.y - 1), 3, 3);
						buffer.drawOval((int) (sd.x - sd.radius), (int) (sd.y - sd.radius), sd.radius * 2, sd.radius * 2);
					}
					if (d instanceof Player)
					{
						Person p = (Person) d;
						// center
						buffer.setColor(Color.green);
						buffer.drawRect((int) (p.x - 1), (int) (p.y - 1), 3, 3);
						// hitbox
						buffer.drawRect((int) (p.x - p.radius), (int) (p.y - p.radius), p.radius * 2, p.radius * 2);
						// radius
						buffer.drawOval((int) (p.x - p.radius), (int) (p.y - p.radius), p.radius * 2, p.radius * 2);
						// target
						buffer.setColor(Color.red);
						buffer.drawOval(p.target.x - 10, p.target.y - 10, 20, 20);
						// id
						buffer.setColor(Color.red);
						buffer.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
						buffer.drawString("" + p.id, (int) p.x, (int) p.y + p.imgH / 2);
						// movement line
						buffer.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
						buffer.setColor(Color.red);
						buffer.drawLine((int) (p.x), (int) (p.y), (int) (p.x + p.strengthOfAttemptedMovement * 100 * Math.cos(p.directionOfAttemptedMovement)),
								(int) (p.y + p.strengthOfAttemptedMovement * 100 * Math.sin(p.directionOfAttemptedMovement)));
						if (p.possessionVessel)
						{
							buffer.setColor(Color.magenta);
							buffer.drawString("VESSEL", (int) p.x - 25, (int) p.y + p.imgH / 2 + 10);
						}
					}
					if (d instanceof NPC)
					{
						NPC p = (NPC) d;
						// center
						buffer.setColor(new Color(160, 255, 160));
						buffer.drawRect((int) (p.x - 1), (int) (p.y - 1), 3, 3);
						// hitbox
						buffer.drawRect((int) (p.x - p.radius), (int) (p.y - p.radius), p.radius * 2, p.radius * 2);
						// radius
						buffer.drawOval((int) (p.x - p.radius), (int) (p.y - p.radius), p.radius * 2, p.radius * 2);
						// target
						buffer.setColor(new Color(255, 160, 160));
						buffer.drawOval(p.target.x - 10, p.target.y - 10, 20, 20);
						// id
						buffer.setColor(Color.red);
						buffer.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
						buffer.drawString("" + p.id, (int) p.x, (int) p.y + p.imgH / 2);
						if (p.possessionVessel)
						{
							buffer.setColor(Color.magenta);
							buffer.drawString("VESSEL", (int) p.x - 25, (int) p.y + p.imgH / 2 + 10);
						}
						// movement line
						buffer.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
						buffer.setColor(Color.red);
						buffer.drawLine((int) (p.x), (int) (p.y), (int) (p.x + p.strengthOfAttemptedMovement * 100 * Math.cos(p.directionOfAttemptedMovement)),
								(int) (p.y + p.strengthOfAttemptedMovement * 100 * Math.sin(p.directionOfAttemptedMovement)));

						if (p.path != null)
						{// path
							buffer.setColor(Color.red);
							buffer.setStroke(new BasicStroke(3));
							for (int i = 0; i < p.path.size() - 1; i++)
								buffer.drawLine(p.path.get(i).x, p.path.get(i).y, p.path.get(i + 1).x, p.path.get(i + 1).y);
							for (int i = 0; i < p.path.size(); i++)
							{
								if (p.path.get(i).byPortal)
									buffer.setColor(Color.PINK);
								else
									buffer.setColor(Color.red);
								buffer.drawRect(p.path.get(i).x - 1, p.path.get(i).y - 1, 3, 3);
							}
						}

						// // other paths (commented out)
						// if (p.strategy == NPC.Strategy.AGGRESSIVE)
						// {
						//
						// buffer.setColor(Color.black);
						// buffer.setStroke(new BasicStroke(1));
						// for (Person p2 : people)
						// if (!p2.equals(p))
						// {
						// buffer.translate(5, 3);
						// List<Point> path = p.pathFind(p2.Point()); // THIS REALLY SHOULDN'T HAPPEN EVERY FRAME TODO TODO TODO
						// if (path != null)
						// for (int i = 0; i < path.size() - 1; i++)
						// buffer.drawLine(path.get(i).x, path.get(i).y, path.get(i + 1).x, path.get(i + 1).y);
						// }
						// buffer.translate(-5 * (people.size() - 1), -3 * (people.size() - 1));
						// }
					}
					if (d instanceof ForceField)
					{
						ForceField ff = (ForceField) d;
						buffer.setColor(Color.red);
						int i = -1;
						for (Point p : ff.p)
						{
							i++;
							buffer.drawRect(p.x - 1, p.y - 1, 3, 3);
							buffer.drawString("" + i, p.x, p.y - 6);
						}
					}
					if (d instanceof ArcForceField)
					{
						ArcForceField aff = (ArcForceField) d;
						buffer.setColor(Color.green);
						buffer.drawArc((int) (aff.x - aff.maxRadius), (int) (aff.y - aff.maxRadius), (int) (aff.maxRadius * 2), (int) (aff.maxRadius * 2),
								(int) (-aff.rotation / Math.PI * 180 - aff.arc / Math.PI * 90), (int) (aff.arc / Math.PI * 180));
						buffer.drawArc((int) (aff.x - aff.minRadius), (int) (aff.y - aff.minRadius), (int) (aff.minRadius * 2), (int) (aff.minRadius * 2),
								(int) (-aff.rotation / Math.PI * 180 - aff.arc / Math.PI * 90), (int) (aff.arc / Math.PI * 180));
						buffer.drawLine((int) (aff.x + aff.minRadius * Math.cos(aff.rotation - 0.5 * aff.arc)), (int) (aff.y + aff.minRadius * Math.sin(aff.rotation - 0.5 * aff.arc)),
								(int) (aff.x + aff.maxRadius * Math.cos(aff.rotation - 0.5 * aff.arc)), (int) (aff.y + aff.maxRadius * Math.sin(aff.rotation - 0.5 * aff.arc)));
						buffer.drawLine((int) (aff.x + aff.minRadius * Math.cos(aff.rotation + 0.5 * aff.arc)), (int) (aff.y + aff.minRadius * Math.sin(aff.rotation + 0.5 * aff.arc)),
								(int) (aff.x + aff.maxRadius * Math.cos(aff.rotation + 0.5 * aff.arc)), (int) (aff.y + aff.maxRadius * Math.sin(aff.rotation + 0.5 * aff.arc)));

						buffer.setColor(Color.red);
						buffer.drawArc((int) (aff.x - (aff.maxRadius + 20)), (int) (aff.y - (aff.maxRadius + 20)), (int) ((aff.maxRadius + 20) * 2), (int) ((aff.maxRadius + 20) * 2),
								(int) (-aff.rotation / Math.PI * 180 - (aff.arc + 2 * 20 / aff.maxRadius) / Math.PI * 90), (int) ((aff.arc + 2 * 20 / aff.maxRadius) / Math.PI * 180));
						buffer.drawArc((int) (aff.x - (aff.minRadius - 20)), (int) (aff.y - (aff.minRadius - 20)), (int) ((aff.minRadius - 20) * 2), (int) ((aff.minRadius - 20) * 2),
								(int) (-aff.rotation / Math.PI * 180 - (aff.arc + 2 * 20 / aff.maxRadius) / Math.PI * 90), (int) ((aff.arc + 2 * 20 / aff.maxRadius) / Math.PI * 180));
						buffer.drawLine((int) (aff.x + (aff.minRadius - 20) * Math.cos(aff.rotation - 0.5 * (aff.arc + 2 * 20 / aff.maxRadius))),
								(int) (aff.y + (aff.minRadius - 20) * Math.sin(aff.rotation - 0.5 * (aff.arc + 2 * 20 / aff.maxRadius))),
								(int) (aff.x + (aff.maxRadius + 20) * Math.cos(aff.rotation - 0.5 * (aff.arc + 2 * 20 / aff.maxRadius))),
								(int) (aff.y + (aff.maxRadius + 20) * Math.sin(aff.rotation - 0.5 * (aff.arc + 2 * 20 / aff.maxRadius))));
						buffer.drawLine((int) (aff.x + (aff.minRadius - 20) * Math.cos(aff.rotation + 0.5 * (aff.arc + 2 * 20 / aff.maxRadius))),
								(int) (aff.y + (aff.minRadius - 20) * Math.sin(aff.rotation + 0.5 * (aff.arc + 2 * 20 / aff.maxRadius))),
								(int) (aff.x + (aff.maxRadius + 20) * Math.cos(aff.rotation + 0.5 * (aff.arc + 2 * 20 / aff.maxRadius))),
								(int) (aff.y + (aff.maxRadius + 20) * Math.sin(aff.rotation + 0.5 * (aff.arc + 2 * 20 / aff.maxRadius))));
					}
				}
			}
		}
	}

	public Area updateVisibility(Person person, int[][] seenBefore)
	{
		final int precision = 720; // honestly even 300 is fine
		final double maxDistance = 100000000;
		final double minDistance = 0;
		final double extra = 70;
		final double visibilityFromAbovePow2 = Math.pow(person.flightVisionDistance * person.z, 2);

		double minX = Math.min(0 + 6, person.x - 6);
		double maxX = Math.max(widthPixels - 1 - 6, person.x + 6);
		double minY = Math.min(0 + 6, person.y - 6);
		double maxY = Math.max(heightPixels - 1 - 6, person.y + 6);
		Line2D top = new Line2D.Double(minX, minY, maxX, minY);
		Line2D left = new Line2D.Double(minX, minY, minX, maxY);
		Line2D bottom = new Line2D.Double(minX, maxY, maxX, maxY);
		Line2D right = new Line2D.Double(maxX, minY, maxX, maxY);
		List<Line2D> boundsLines = new ArrayList<Line2D>();
		boundsLines.add(top);
		boundsLines.add(left);
		boundsLines.add(bottom);
		boundsLines.add(right);

		Polygon visibleAreaPolygon = new Polygon();
		for (double angle = 0; angle <= TAU; angle += TAU / precision)
		{
			Point2D start = new Point2D.Double(person.x + minDistance * Math.cos(angle), person.y + minDistance * Math.sin(angle));
			Line2D line = new Line2D.Double(start.getX(), start.getY(), start.getX() + maxDistance * Math.cos(angle), start.getY() + maxDistance * Math.sin(angle));
			Point2D closestPoint = null;
			double shortestDistPow2 = Double.MAX_VALUE;
			// bounds intersection first
			for (Line2D l : boundsLines)
			{
				Point2D closest = Methods.getSegmentIntersection(line, l);
				if (closest != null)
				{
					double distPow2 = Methods.DistancePow2(start, closest);
					if (distPow2 < shortestDistPow2)
					{
						shortestDistPow2 = distPow2;
						line.setLine(start, closest);
						closestPoint = closest;
					}
				}
			}
			// walls
			for (int x = 0; x < width; x++)
				if (x * squareSize > minX - squareSize && x * squareSize < maxX)
					for (int y = 0; y < height; y++)
						if (y * squareSize > minY - squareSize && y * squareSize < maxY)
							if (wallTypes[x][y] != -1) // TODO check for transparent walls if there exist any
								if (wallTypes[x][y] == -2
										|| Methods.DistancePow2(x * squareSize + squareSize / 2, y * squareSize + squareSize / 2, start.getX(), start.getY()) > visibilityFromAbovePow2)
								{
									if (wallTypes[x][y] == 11) // plant walls are transparent
										continue;
									Rectangle2D wallRect = new Rectangle2D.Double(x * squareSize, y * squareSize, squareSize, squareSize);
									if (line.intersects(wallRect))
									{
										Point2D closest = Methods.getClosestIntersectionPoint(line, wallRect);
										if (closest != null)
										{
											double distPow2 = Methods.DistancePow2(start, closest);
											if (distPow2 < shortestDistPow2)
											{
												shortestDistPow2 = distPow2;
												line.setLine(start, closest);
												closestPoint = closest;
											}
										}
									}
								}

			if (closestPoint != null)
			{
				// update seenBefore
				int x = (int) (closestPoint.getX() + 1 * Math.cos(angle)) / squareSize;
				x = Math.max(0, x);
				x = Math.min(x, width - 1);
				int y = (int) (closestPoint.getY() + 1 * Math.sin(angle)) / squareSize;
				y = Math.max(0, y);
				y = Math.min(y, height - 1);
				seenBefore[x][y] = 1;

				visibleAreaPolygon.addPoint((int) (closestPoint.getX() + extra * Math.cos(angle)), (int) (closestPoint.getY() + extra * Math.sin(angle)));
			}
		}
		return new Area(visibleAreaPolygon);
	}

	public void addWall(int x, int y, int elementalType, boolean fullHealth)
	{
		if (x < 0 || y < 0 || x >= width || y >= height)
			return;
		if (poolTypes[x][y] != -1 || (wallTypes[x][y] != -1 && fullHealth))
			remove(x, y);
		wallTypes[x][y] = elementalType;
		if (fullHealth)
			wallHealths[x][y] = 100;
		checkWCorner(elementalType, x, y);
		checkWCorner(elementalType, x + 1, y);
		checkWCorner(elementalType, x, y + 1);
		checkWCorner(elementalType, x + 1, y + 1);
	}

	/**
	 * Adds a full health wall without removing walls or updating corners. After doing this a lot of times, do updateAllWallCorners() Will not add if it's -6174
	 * 
	 * @param x
	 * @param y
	 * @param elementalType
	 */
	public void addWall(int x, int y, int elementalType)
	{
		if (x < 0 || y < 0 || x >= width || y >= height)
			return;
		if (wallTypes[x][y] == -6174)
			return;
		wallTypes[x][y] = elementalType;
		wallHealths[x][y] = 100;
	}

	public void addPool(int x, int y, int elementalType, boolean fullHealth)
	{
		if (x < 0 || y < 0 || x >= width || y >= height)
			return;
		if ((wallTypes[x][y] != -1 || poolTypes[x][y] != -1) && !fullHealth)
			return; // can't create where there's already something
		for (Person p : people)
			if ((int) (p.x - p.radius) / squareSize == x || (int) (p.x + p.radius) / squareSize == x)
				if ((int) (p.y - p.radius) / squareSize == y || (int) (p.y + p.radius) / squareSize == y)
					return;
		poolTypes[x][y] = elementalType;
		if (fullHealth)
			poolHealths[x][y] = 100;
		checkPCorner(elementalType, x, y);
		checkPCorner(elementalType, x + 1, y);
		checkPCorner(elementalType, x, y + 1);
		checkPCorner(elementalType, x + 1, y + 1);
		updatePools();
	}

	/**
	 * Removes without checking or updating stuff
	 * 
	 * @param x
	 * @param y
	 */
	public void removeFast(int x, int y)
	{
		wallTypes[x][y] = -1;
		wallHealths[x][y] = -1;
		poolTypes[x][y] = -1;
		poolHealths[x][y] = -1;
	}

	public boolean remove(int x, int y)
	{
		if (x < 0 || y < 0 || x >= width || y >= height)
			return false;
		int element = -1;
		if (wallTypes[x][y] != -1)
		{
			element = wallTypes[x][y];
			wallTypes[x][y] = -1;
			wallHealths[x][y] = -1;
			updateWallCorners(x, y, element);
			return true;
		}
		if (poolTypes[x][y] != -1)
		{
			element = poolTypes[x][y];
			poolTypes[x][y] = -1;
			poolHealths[x][y] = -1;
			updatePoolCorners(x, y, element);
			updatePools();
			return true;
		}
		return false;
	}

	public void updateAllWallCorners()
	{
		for (int i = 0; i < wCornerStyles.length; i++)
			for (int j = 0; j < wCornerStyles[0].length; j++)
				for (int e = 0; e < wCornerStyles[0][0].length; e++)
					checkWCorner(e, i, j);
	}

	public void updateWallCorners(int x, int y, int element)
	{
		checkWCorner(element, x, y);
		checkWCorner(element, x + 1, y);
		checkWCorner(element, x, y + 1);
		checkWCorner(element, x + 1, y + 1);
	}

	public void updatePoolCorners(int x, int y, int element)
	{
		checkPCorner(element, x, y);
		checkPCorner(element, x + 1, y);
		checkPCorner(element, x, y + 1);
		checkPCorner(element, x + 1, y + 1);
	}

	public void destroyWall(int x, int y)
	{
		int elementNum = wallTypes[x][y];
		if (remove(x, y))
			otherDebris(x, y, getWallDebrisType(elementNum), "destroy", 0);
	}

	public void destroyPool(int x, int y)
	{
		int elementNum = poolTypes[x][y];
		if (remove(x, y))
			otherDebris(x, y, getPoolDebrisType(elementNum), "destroy", 0);
	}

	public void connectPool(int x, int y)
	{
		int element = -1;
		if (poolTypes[x][y] != -1)
		{
			element = poolTypes[x][y];
			checkPCorner(element, x, y);
			checkPCorner(element, x + 1, y);
			checkPCorner(element, x, y + 1);
			checkPCorner(element, x + 1, y + 1);
			updatePools();
		}
	}

	public void connectWall(int x, int y)
	{
		int element = -1;
		if (wallTypes[x][y] != -1)
		{
			element = wallTypes[x][y];
			checkWCorner(element, x, y);
			checkWCorner(element, x + 1, y);
			checkWCorner(element, x, y + 1);
			checkWCorner(element, x + 1, y + 1);
		}
	}

	void checkWCorner(int e, int x, int y)
	{
		if (e == -2)
			return;
		if (x <= width - 1 && y <= height - 1 && x >= 1 && y >= 1)
		{
			boolean a = wallTypes[x - 1][y - 1] == e; // LU
			boolean b = wallTypes[x][y - 1] == e; // RU
			boolean c = wallTypes[x - 1][y] == e; // LD
			boolean d = wallTypes[x][y] == e; // RD
			int numOfTakens = (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) + (d ? 1 : 0);
			if (numOfTakens < 2)
			{
				wCornerStyles[x][y][e] = -1;
				cornerCracks[x][y] = -1;
			}
			else
			{
				// damage crack images
				int damageNumbers = 0;
				if (a)
					damageNumbers += wallHealths[x - 1][y - 1];
				if (b)
					damageNumbers += wallHealths[x][y - 1];
				if (c)
					damageNumbers += wallHealths[x - 1][y];
				if (d)
					damageNumbers += wallHealths[x][y];
				damageNumbers /= numOfTakens;
				damageNumbers /= 25;
				if (damageNumbers >= 3)
					damageNumbers = -1;
				else
					damageNumbers = 2 - damageNumbers;
				cornerCracks[x][y] = damageNumbers;

				if (a && b && !c && !d)
					wCornerStyles[x][y][e] = 0; // up
				else if (!a && b && !c && d)
					wCornerStyles[x][y][e] = 1; // right
				else if (!a && !b && c && d)
					wCornerStyles[x][y][e] = 2; // down
				else if (a && !b && c && !d)
					wCornerStyles[x][y][e] = 3; // left

				else if (a && !b && !c && d)
					wCornerStyles[x][y][e] = 4; // bridge 1
				else if (!a && b && c && !d)
					wCornerStyles[x][y][e] = 5; // bridge 2

				else if (a && b && c && !d)
					wCornerStyles[x][y][e] = 6; // all except RD
				else if (a && b && !c && d)
					wCornerStyles[x][y][e] = 7; // all except LD
				else if (!a && b && c && d)
					wCornerStyles[x][y][e] = 8; // all except LU
				else if (a && !b && c && d)
					wCornerStyles[x][y][e] = 9; // all except RU

				else if (a && b && c && d)
					wCornerStyles[x][y][e] = 10;
			}
		}
		// else, I don't have time for this shit
	}

	void checkPCorner(int e, int x, int y)
	{
		if (x < width - 1 && y < height - 1 && x > 0 && y > 0)
		{
			boolean a = poolTypes[x - 1][y - 1] == e;
			boolean b = poolTypes[x][y - 1] == e;
			boolean c = poolTypes[x - 1][y] == e;
			boolean d = poolTypes[x][y] == e;
			int numOfTakens = (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) + (d ? 1 : 0);
			if (numOfTakens < 2)
				pCornerStyles[x][y][e] = -1;
			else if (a && b && !c && !d)
				pCornerStyles[x][y][e] = 0;
			else if (!a && b && !c && d)
				pCornerStyles[x][y][e] = 1;
			else if (!a && !b && c && d)
				pCornerStyles[x][y][e] = 2;
			else if (a && !b && c && !d)
				pCornerStyles[x][y][e] = 3;

			else if (a && !b && !c && d)
				pCornerStyles[x][y][e] = 4;
			else if (!a && b && c && !d)
				pCornerStyles[x][y][e] = 5;

			else if (a && b && c && !d)
				pCornerStyles[x][y][e] = 6;
			else if (a && b && !c && d)
				pCornerStyles[x][y][e] = 7;
			else if (!a && b && c && d)
				pCornerStyles[x][y][e] = 8;
			else if (a && !b && c && d)
				pCornerStyles[x][y][e] = 9;

			else if (a && b && c && d)
				pCornerStyles[x][y][e] = 10;
		}
		// else, I don't have time for this shit
	}

	static int getCornerAngle(int n)
	{
		int ans = 0;
		if (n < 4)
			ans = n;
		if (n >= 4)
			ans = n - 4;
		if (n >= 6)
			ans = n - 6;
		if (n == 10)
			return 0;
		return ans;
	}

	static int getCornerStyle(int n)
	{
		if (n < 4)
			return 0;
		if (n < 6)
			return 1;
		if (n < 10)
			return 2;
		else
			return 3;
	}

	private static int lastIDgiven = 0;

	public static int giveID()
	{
		if (lastIDgiven >= Integer.MAX_VALUE)
		{
			MAIN.errorMessage("HAHAHAHAHAHAHAHA what the fuck?");
			lastIDgiven = Integer.MIN_VALUE;
		}
		return lastIDgiven++;
	}

	public static void resetIDs()
	{
		lastIDgiven = 0;
	}

	static long getSeed(Environment e)
	{
		Long l = (long) Integer.hashCode(7 * e.globalX * e.globalY);
		return l;
	}

	public void tempBuild()
	{
		Random random = new Random(Environment.getSeed(this));
		// floor
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
			{
				floorTypes[i][j] = 0;
				// if (i == 0 || j == 0 || i == width - 1 || j == height - 1)
				// {
				// if (random.nextDouble() > 0.05 || (i == 0 && j == 0) || (i == width - 1 && j == 0) || (i == 0 && j == height - 1) || (i == width - 1 && j == height - 1)) // 5% of an opening if this is not a corner
				// addWall(i, j, -2, true);
				// }
			}

		// Shadow direction and distance of every object
		shadowX = 1;
		shadowY = -0.7;

		cityBlockGen(random);

		// Cut open all marked doors
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				if (wallTypes[i][j] == -6174)
				{
					wallTypes[i][j] = 5;
					wallHealths[i][j] = 100;
				}

		updateAllWallCorners();
	}

	/**
	 * for a 48x48 Environment
	 * 
	 * @param random
	 */
	void cityBlockGen(Random random)
	{
		final int shift = 3;
		int asphalt = 5;
		int sidewalk = 6;

		Point[] pos = new Point[]
		{ new Point(0, 0), new Point(24, 0), new Point(0, 24), new Point(24, 24) };
		for (int i = 0; i < 4; i++)
			randomBlock(pos[i].x, pos[i].y, random);

		// Streets
		// vertical
		for (int y = 0; y < height; y++)
		{
			for (int x = 18; x <= 22; x++)
				floorTypes[x][y] = asphalt;
			for (int x = 42; x <= 46; x++)
				floorTypes[x][y] = asphalt;
		}
		// horizontal
		for (int x = 0; x < width; x++)
		{
			for (int y = 18; y <= 22; y++)
				floorTypes[x][y] = asphalt;
			for (int y = 42; y <= 46; y++)
				floorTypes[x][y] = asphalt;
		}

		// Sidewalks
		// left
		for (int x = 0; x <= 17; x++)
		{
			floorTypes[x][17] = sidewalk;
			floorTypes[x][23] = sidewalk;
		}
		// up
		for (int y = 0; y <= 17; y++)
		{
			floorTypes[17][y] = sidewalk;
			floorTypes[23][y] = sidewalk;
		}
		// right
		for (int x = 23; x < 42; x++)
		{
			floorTypes[x][17] = sidewalk;
			floorTypes[x][23] = sidewalk;
		}
		// down
		for (int y = 23; y < 42; y++)
		{
			floorTypes[17][y] = sidewalk;
			floorTypes[23][y] = sidewalk;
		}
		// bottom bottom left
		for (int x = 0; x <= 17; x++)
		{
			floorTypes[x][41] = sidewalk;
			floorTypes[x][47] = sidewalk;
		}
		// bottom bottom right
		for (int x = 23; x < 42; x++)
		{
			floorTypes[x][41] = sidewalk;
			floorTypes[x][47] = sidewalk;
		}
		// right right up
		for (int y = 0; y <= 17; y++)
		{
			floorTypes[41][y] = sidewalk;
			floorTypes[47][y] = sidewalk;
		}
		// right right down
		for (int y = 23; y < 42; y++)
		{
			floorTypes[41][y] = sidewalk;
			floorTypes[47][y] = sidewalk;
		}
		// teeny tiny bottomest rightest point
		floorTypes[47][47] = sidewalk;

		// SHIFT EVERYTHING
		int[][] shiftedFloorTypes = new int[width][height];
		int[][] shiftedWallTypes = new int[width][height];
		int[][] shiftedWallHealths = new int[width][height];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
			{
				shiftedFloorTypes[(x + shift) % width][(y + shift) % height] = floorTypes[x][y];
				shiftedWallTypes[(x + shift) % width][(y + shift) % height] = wallTypes[x][y];
				shiftedWallHealths[(x + shift) % width][(y + shift) % height] = wallHealths[x][y];
			}
		//
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
			{
				floorTypes[x][y] = shiftedFloorTypes[x][y];
				wallTypes[x][y] = shiftedWallTypes[x][y];
				wallHealths[x][y] = shiftedWallHealths[x][y];
			}
		for (Furniture f : furniture)
		{
			f.x = (f.x + shift * squareSize) % widthPixels;
			f.y = (f.y + shift * squareSize) % heightPixels;
		}
	}

	enum Block
	{
		NORMAL, PARK
	};

	void randomBlock(int startX, int startY, Random random)
	{
		Block block = Block.values()[random.nextInt(Block.values().length)];
		switch (block)
		{
		case NORMAL:
			normalBuilding(startX, startY, random);
			break;
		case PARK:
			park(startX, startY, random);
			break;
		default:
			MAIN.errorMessage("stop parturing the derkagons, shleem!");
			break;
		}
	}

	void park(int startX, int startY, Random random)
	{
		int dirt = 0;
		int pavement = 4;
		int grass = 3;

		int size = 16;

		// pavement + big square of grass
		for (int i = 0; i <= size; i++)
			for (int j = 0; j <= size; j++)
			{
				if (i == 0 || j == 0 || i == size || j == size)
					floorTypes[startX + i][startY + j] = pavement;
				else
					floorTypes[startX + i][startY + j] = grass;
			}

		// paths
		Point[] p = new Point[]
		{ new Point(-1, 0), new Point(1, 0), new Point(0, -1), new Point(0, 1) };
		int numOfPaths = 4;
		for (int k = 0; k < numOfPaths; k++)
		{
			Point snakeHead = new Point(startX + 2 + random.nextInt(size - 4), startY + 2 + random.nextInt(size - 4));
			int count = 0;
			while (snakeHead.x < startX + size && snakeHead.y < startY + size && snakeHead.x > startX && snakeHead.y > startY && count < 30)
			{
				count++;
				floorTypes[snakeHead.x][snakeHead.y] = 6666;
				Point next = new Point(-1, -1); // doesn't matter
				boolean bool = false;
				int attempts = 0;
				while (!bool && attempts < 9)
				{
					attempts++;
					bool = true;
					int i = random.nextInt(4);
					next = new Point(snakeHead.x + p[i].x, snakeHead.y + p[i].y);
					int neighbors = 0;
					for (int j = 0; j < 4; j++)
					{
						if (next.x + p[j].x <= 0 || next.y + p[j].y <= 0)
							continue;
						if (floorTypes[next.x + p[j].x][next.y + p[j].y] == 6666)
							neighbors++;
					}
					if (neighbors != 1)
						bool = false;
				}
				snakeHead = next;
			}
			for (int i = startX; i <= startX + size; i++)
				for (int j = startY; j <= startY + size; j++)
					if (floorTypes[i][j] == 6666)
						floorTypes[i][j] = dirt;
		}
	}

	void normalBuilding(int startX, int startY, Random random)
	{
		int cement = 12;
		final int roomFloor = 1;
		final int roomFloorEdge = 2;
		int size = 16;
		int tempFloorUnderFurniture = 234128756;

		for (int x = 0; x < size; x++)
		{
			floorTypes[startX + x][startY + 0] = roomFloorEdge;
			addWall(startX + x, startY + 0, cement);
		}
		for (int y = 0; y < size; y++)
		{
			floorTypes[startX + 0][startY + y] = roomFloorEdge;
			addWall(startX + 0, startY + y, cement);
		}
		for (int x = 0; x < size; x++)
		{
			floorTypes[startX + x][startY + size] = roomFloorEdge;
			addWall(startX + x, startY + size, cement);
		}
		for (int y = 0; y < size; y++)
		{
			floorTypes[startX + size][startY + y] = roomFloorEdge;
			addWall(startX + size, startY + y, cement);
		}
		addWall(startX + size, startY + size, cement);
		floorTypes[startX + size][startY + size] = roomFloorEdge;
		for (int x = 1; x <= size - 1; x++)
			for (int y = 1; y <= size - 1; y++)
				floorTypes[startX + x][startY + y] = roomFloor;

		// 0-2 doors on outer sides
		int numOfDoors = random.nextInt(4);
		for (int i = 0; i < numOfDoors; i++)
		{
			int pos = 1 + random.nextInt(size - 2);
			switch (random.nextInt(4)) // side
			{
			case 0:
				removeFast(startX + 0, startY + pos);
				floorTypes[startX + 0][startY + pos] = tempFloorUnderFurniture;
				furniture.add(new Furniture(startX * squareSize + 48, (startY + pos) * squareSize + 48, "door", Math.PI / 2 * 1));
				break;
			case 1:
				removeFast(startX + pos, startY + 0);
				floorTypes[startX + pos][startY + 0] = tempFloorUnderFurniture;
				furniture.add(new Furniture((startX + pos) * squareSize + 48, (startY + 0) * squareSize + 48, "door", Math.PI / 2 * 2));
				break;
			case 2:
				removeFast(startX + size, startY + pos);
				floorTypes[startX + size][startY + pos] = tempFloorUnderFurniture;
				furniture.add(new Furniture((startX + size) * squareSize + 48, (startY + pos) * squareSize + 48, "door", Math.PI / 2 * 3));
				break;
			case 3:
				removeFast(startX + pos, startY + size);
				floorTypes[startX + pos][startY + size] = tempFloorUnderFurniture;
				furniture.add(new Furniture((startX + pos) * squareSize + 48, (startY + size) * squareSize + 48, "door", 0));
				break;
			default:
				MAIN.errorMessage(".");
				break;
			}
		}

		// split into rooms
		int amountOfSplits = random.nextInt(5);
		if (amountOfSplits == 1)
			amountOfSplits = 2;
		boolean[] existing = new boolean[4]; // left up right down
		for (int i = 0; i < amountOfSplits; i++)
		{
			int side;
			// make sure it's not repeating an already chose side
			do
				side = random.nextInt(4);
			while (existing[side]);
			existing[side] = true;
			boolean door = random.nextBoolean();
			int pos = 1 + random.nextInt(size / 2 - 2); // door
			switch (side)
			{
			case 0: // left
				for (int x = 1; x < size / 2 + 1; x++)
				{
					floorTypes[startX + x][startY + size / 2] = roomFloorEdge;
					addWall(startX + x, startY + size / 2, cement);
				}
				if (door)
				{
					removeFast(startX + pos, startY + size / 2);
					floorTypes[startX + pos][startY + size / 2] = roomFloor;
				}
				break;
			case 1: // up
				for (int y = 1; y < size / 2 + 1; y++)
				{
					floorTypes[startX + size / 2][startY + y] = roomFloorEdge;
					addWall(startX + size / 2, startY + y, cement);
				}
				if (door)
				{
					removeFast(startX + size / 2, startY + pos);
					floorTypes[startX + size / 2][startY + pos] = roomFloor;
				}
				break;
			case 2: // right
				for (int x = 0; x < size / 2 + 1; x++)
				{
					floorTypes[startX + size / 2 + x][startY + size / 2] = roomFloorEdge;
					addWall(startX + size / 2 + x, startY + size / 2, cement);
				}
				if (door)
				{
					removeFast(startX + size / 2 + pos, startY + size / 2);
					floorTypes[startX + size / 2 + pos][startY + size / 2] = roomFloor;
				}
				break;
			case 3:
				for (int y = 0; y < size / 2 + 1; y++)
				{
					floorTypes[startX + size / 2][startY + size / 2 + y] = roomFloorEdge;
					addWall(startX + size / 2, startY + size / 2 + y, cement);
				}
				if (door)
				{
					removeFast(startX + size / 2, startY + size / 2 + pos);
					floorTypes[startX + size / 2][startY + size / 2 + pos] = roomFloor;
				}
				break;
			default:
				MAIN.errorMessage(".");
				break;
			}
		}

		// Furniture!
		for (int x = startX + 1; x < startX + size; x++)
			for (int y = startY + 1; y < startY + size; y++)
				if (floorTypes[x][y] == roomFloor && wallTypes[x][y] == -1)
				{
					if (random.nextDouble() < 0.02) // 2% chance of chair
					{
						double angle = random.nextInt(4) * Math.PI * 0.5;
						floorTypes[x][y] = tempFloorUnderFurniture;
						furniture.add(new Furniture(x * squareSize + 48, y * squareSize + 48, "wood_chair", angle));
					}
					else if (random.nextDouble() < 0.01) // 1% chance of plant pot
					{
						double angle = random.nextInt(4) * Math.PI * 0.5;
						floorTypes[x][y] = tempFloorUnderFurniture;
						furniture.add(new Furniture(x * squareSize + 48, y * squareSize + 48, "plant_pot", angle));
					}
					else if (random.nextDouble() < 0.01) // 1% chance of desk
					{
						double angle = random.nextInt(4) * Math.PI * 0.5;
						int otherX = (int) (x + Math.cos(angle));
						int otherY = (int) (y + Math.sin(angle));
						if (floorTypes[otherX][otherY] == roomFloor)
						{
							floorTypes[x][y] = tempFloorUnderFurniture;
							floorTypes[otherX][otherY] = tempFloorUnderFurniture;
							furniture.add(new Furniture(x * squareSize + 48 + 48 * Math.cos(angle), y * squareSize + 48 + 48 * Math.sin(angle), "desk", angle));
						}
					}
				}
		for (int x = startX; x <= startX + size; x++)
			for (int y = startY; y <= startY + size; y++)
				if (floorTypes[x][y] == tempFloorUnderFurniture)
					floorTypes[x][y] = roomFloor;
	}

	void villageGen(Random random)
	{
		int cement = 12;
		int roomFloor = 1;
		int roomFloorEdge = 2;
		int noFloor = 0;
		int pavement = 1;

		List<Rectangle> rooms = new ArrayList<Rectangle>();
		// rooms
		for (int i = 0; i < 30; i++)
		{
			int w = random.nextInt(8) + 4;
			int h = random.nextInt(8) + 4;
			int x = random.nextInt(width - w);
			int y = random.nextInt(height - h);
			Rectangle room = new Rectangle(x, y, w, h);
			boolean no = false;
			for (Rectangle r : rooms)
				if (r.intersects(room))
					no = true;
			if (!no)
			{
				for (int xx = room.x; xx <= room.x + room.width; xx++)
				{
					floorTypes[xx][room.y] = roomFloorEdge;
					addWall(xx, room.y, cement);
				}
				for (int xx = room.x; xx <= room.x + room.width; xx++)
				{
					floorTypes[xx][room.y + room.height] = roomFloorEdge;
					addWall(xx, room.y + room.height, cement);
				}
				for (int yy = room.y; yy <= room.y + room.height; yy++)
				{
					floorTypes[room.x][yy] = roomFloorEdge;
					addWall(room.x, yy, cement);
				}
				for (int yy = room.y; yy <= room.y + room.height; yy++)
				{
					floorTypes[room.x + room.width][yy] = roomFloorEdge;
					addWall(room.x + room.width, yy, cement);
				}

				// floor
				for (int xx = x + 1; xx < x + w; xx++)
					for (int yy = y + 1; yy < y + h; yy++)
						floorTypes[xx][yy] = roomFloor;

				rooms.add(room);
			}
		}

		// lines
		for (int i = 0; i < 10; i++)
		{
			int w = random.nextInt(12) + 3;
			int x = random.nextInt(width - w);
			int y = random.nextInt(height);
			for (int xx = x; xx <= x + w; xx++)
				addWall(xx, y, cement);
		}
		for (int i = 0; i < 10; i++)
		{
			int h = random.nextInt(12) + 3;
			int x = random.nextInt(width);
			int y = random.nextInt(height - h);
			for (int yy = y; yy <= y + h; yy++)
				addWall(x, yy, cement);
		}

		// remove bulbs until there are none
		List<Point> badPlaces = new ArrayList<Point>();
		int[][] scores = new int[width][height];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				scores[x][y] = 0;
		for (int x = 1; x < width - 1; x++)
			for (int y = 1; y < height - 1; y++)
				for (int x2 = x - 1; x2 <= x + 1; x2++)
					for (int y2 = y - 1; y2 <= y + 1; y2++)
						scores[x2][y2] += (wallTypes[x][y] == -1 ? 0 : 1);
		for (int x = 1; x < width - 1; x++)
			for (int y = 1; y < height - 1; y++)
				if (wallTypes[x][y] > 0 && scores[x][y] == 2)
					badPlaces.add(new Point(x, y));
		while (!badPlaces.isEmpty())
		{
			Point spot = badPlaces.get(0);
			removeFast(spot.x, spot.y);
			for (int x2 = spot.x - 1; x2 <= spot.x + 1; x2++)
				for (int y2 = spot.y - 1; y2 <= spot.y + 1; y2++)
				{
					scores[x2][y2]--;
					if (x2 > 1 && y2 > 1 && x2 < width - 1 && y2 < height - 1)
						if (wallTypes[x2][y2] >= 0)
						{
							int directlyAdjacents = 0 + (wallTypes[x2 - 1][y2] >= 0 ? 1 : 0) + (wallTypes[x2 + 1][y2] >= 0 ? 1 : 0) + (wallTypes[x2][y2 + 1] >= 0 ? 1 : 0)
									+ (wallTypes[x2][y2 - 1] >= 0 ? 1 : 0);
							// OOO
							// OXX
							// OOO
							if (scores[x2][y2] == 2)
								badPlaces.add(1, new Point(x2, y2));
							// OOX
							// OXX
							// OOX
							if (scores[x2][y2] == 4)
								if (directlyAdjacents == 1)
									badPlaces.add(1, new Point(x2, y2));
							// OOX
							// OXX
							// OOO
							if (scores[x2][y2] == 3)
								if (directlyAdjacents == 1)
									badPlaces.add(1, new Point(x2, y2));
						}
				}
			badPlaces.remove(0);
		}

		// add random openings in "walls"
		for (int x = 1; x < width - 1; x++)
			for (int y = 1; y < height - 1; y++)
				if (wallTypes[x][y] > 0 && scores[x][y] == 3) // two adjacent walls
					if ((wallTypes[x - 1][y] >= 0 && wallTypes[x + 1][y] >= 0) || (wallTypes[x][y - 1] >= 0 && wallTypes[x][y + 1] >= 0)) // not corners
						if (random.nextDouble() < 0.12) // chance of door
							removeFast(x, y);

		// Pathways! Fun!
		for (int i = 0; i < 40; i++)
		{
			ProceduralGenerationMap map = new ProceduralGenerationMap(width, height, rooms, wallTypes);
			PathFinder pf = new ProcGenPathFinder(map, width + height, false);

			Rectangle r1 = rooms.get(random.nextInt(rooms.size()));
			Rectangle r2 = rooms.get(random.nextInt(rooms.size()));
			Path path = pf.findPath(null, r1.x + r1.width / 2, r1.y + r1.height / 2, r2.x + r2.width / 2, r2.y + r2.height / 2);
			if (path != null)
				for (int j = 0; j < path.getLength(); j++)
					if (floorTypes[path.getX(j)][path.getY(j)] == noFloor || floorTypes[path.getX(j)][path.getY(j)] == roomFloorEdge)
						floorTypes[path.getX(j)][path.getY(j)] = pavement;
		}
	}

	public void removeAroundPerson(Person p)
	{
		remove((int) (p.x - p.radius) / squareSize, (int) (p.y - p.radius) / squareSize);
		remove((int) (p.x - p.radius) / squareSize, (int) (p.y + p.radius) / squareSize);
		remove((int) (p.x + p.radius) / squareSize, (int) (p.y - p.radius) / squareSize);
		remove((int) (p.x + p.radius) / squareSize, (int) (p.y + p.radius) / squareSize);
	}

	public boolean checkPortal(Portal p1)
	{
		// boundary tests
		if (p1.start.x < 0 || p1.start.y < 0 || p1.start.x >= widthPixels || p1.start.y >= heightPixels)
			return false;
		if (p1.end.x < 0 || p1.end.y < 0 || p1.end.x >= widthPixels || p1.end.y >= heightPixels)
			return false;

		// wall tests
		if (p1.z <= 1)
		{
			int minX = Math.min(Math.max((int) (p1.x - p1.length / 2) / squareSize, 0), width - 1);
			int maxX = Math.min(Math.max((int) (p1.x + p1.length / 2) / squareSize, 0), width - 1);
			int minY = Math.min(Math.max((int) (p1.y - p1.length / 2) / squareSize, 0), height - 1);
			int maxY = Math.min(Math.max((int) (p1.y + p1.length / 2) / squareSize, 0), height - 1);
			for (int x = minX; x <= maxX; x++)
				for (int y = minY; y <= maxY; y++)
					if (wallTypes[x][y] != -1)
						if (Methods.SegmentToPointDistancePow2(p1.start.x, p1.start.y, p1.end.x, p1.end.y, x * squareSize + squareSize / 2, y * squareSize + squareSize / 2) < squareSize / 2
								* squareSize / 2)
							return false;
		}

		for (Portal p2 : portals)
			if (p1.z <= p2.highestPoint() && p2.z <= p1.highestPoint())
			{
				if (p1.Line2D().intersectsLine(p2.Line2D()))
					return false;
				if (Methods.SegmentToPointDistancePow2(p2.start.x, p2.start.y, p2.end.x, p2.end.y, p1.start.x, p1.start.y) < Portals.minimumDistanceBetweenPortalsPow2)
					return false;
				if (Methods.SegmentToPointDistancePow2(p2.start.x, p2.start.y, p2.end.x, p2.end.y, p1.end.x, p1.end.y) < Portals.minimumDistanceBetweenPortalsPow2)
					return false;
				if (Methods.SegmentToPointDistancePow2(p1.start.x, p1.start.y, p1.end.x, p1.end.y, p2.start.x, p2.start.y) < Portals.minimumDistanceBetweenPortalsPow2)
					return false;
				if (Methods.SegmentToPointDistancePow2(p1.start.x, p1.start.y, p1.end.x, p1.end.y, p2.end.x, p2.end.y) < Portals.minimumDistanceBetweenPortalsPow2)
					return false;
			}
		return true;
	}

	public void personPunchWall(Person user, double leftoverPushback, int wallType)
	{
		int element = -1;
		switch (wallType)
		{
		case -2: // edge walls
		case 12: // cement
			element = 10;
			break;
		default:
			element = wallType;
			break;
		}
		hitPerson(user, Math.max(0, 7 - 0.5 * user.STRENGTH), leftoverPushback, user.rotation - Math.PI, element);
	}
}
