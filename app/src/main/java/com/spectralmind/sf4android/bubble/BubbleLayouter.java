package com.spectralmind.sf4android.bubble;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.PointF;
import android.util.FloatMath;

import com.google.common.collect.Lists;
import com.spectralmind.sf4android.definitions.ClusterAttributeDefinition;
import com.spectralmind.sf4android.media.MediaGroup;
import com.spectralmind.sf4android.media.MediaItem;

public class BubbleLayouter {
	private static final Logger LOGGER = LoggerFactory.getLogger(BubbleLayouter.class);

	private static final int MAX_ITERATIONS = 1500;
	private final float MAX_TAGS = 20;
	private final float MAX_SIZE = 300;
	private static final float PARENT_CHILD_RADIUS_FACTOR = 0.60f;	
	private final Random random;

	public BubbleLayouter() {
		this.random = new Random(1);
	}

	public List<Bubble> layoutBubbles(List<Bubble> bubbles, float radius) {
		if(bubbles.isEmpty()) {
			return bubbles;
		}

		if(bubbles.size() == 1) {
			bubbles.get(0).setCenter(new PointF(0, 0));
			return bubbles;
		}

		List<Bubble> bubblesToAvoid = Lists.newArrayList();
		List<Bubble> sortedBubbles = Lists.newArrayList(bubbles);
		Collections.sort(sortedBubbles, new Comparator<Bubble>() {
			@Override
			public int compare(Bubble lhs, Bubble rhs) {
				return -1 * Float.compare(lhs.getRadius(), rhs.getRadius());
			}
		});

		for(Bubble bubble : sortedBubbles) {
			setOriginForBubble(bubble, radius, bubblesToAvoid);
			bubblesToAvoid.add(bubble);
		}

		return sortedBubbles;
	}

	void setOriginForBubble(Bubble bubble, float radius, List<Bubble> bubblesToAvoid) {
		boolean collisionDetected;
		int iterationCnt = 0;
		do {
			collisionDetected = false;
			++iterationCnt;
			bubble.setCenter(randomPositionWithinRadius(radius - bubble.getRadius()));
			for(Bubble collisionCandidate : bubblesToAvoid) {
				if(bubble.collidesWith(collisionCandidate)) {
					collisionDetected = true;
					break;
				}
			}
		} while(collisionDetected && iterationCnt < MAX_ITERATIONS);
	}

	private PointF randomPositionWithinRadius(float radius) {
		if(radius < 0) {
			return new PointF(0, 0);
		}
		float r = (float) ((random.nextFloat() * radius * 3/4) + radius/4);
		float phi = (float) (random.nextFloat() * 2 * Math.PI);
		return new PointF(r * FloatMath.cos(phi), r * FloatMath.sin(phi));
	}
	

	public float radiusForNumTracks(int numTracks, int parentTracks, float maxRadius) {
		return (float) (Math.sqrt(numTracks / (float) parentTracks) * maxRadius);
	}
	
	private int getSumTracks(Collection<? extends MediaGroup<?>> coll) {
		int result = 0;
		for(MediaGroup<?> item : coll) {
			result += item.getNumTracks();
		}
		return result;
	}
	
	public List<Bubble>  createBubbles(List<? extends MediaGroup<?>> coll, List<ClusterAttributeDefinition> attrDefs){
		List<Bubble> bubbles = Lists.newArrayList();
		
		int sumTracks = getSumTracks(coll);
		//		Mood other = null;
		LOGGER.debug("Loaded library with {} tracks", sumTracks);
		int attrIndex = 0;
		int c = 0;
		for(MediaGroup<?> item : coll) {
			//			if(mood.getName().intern() == "Other")
			//			{
			//				other = mood;
			//				continue;
			//			}
			//			if(i == 14 && other != null)
			//				mood = other;
//			if(attrIndex == MAX_TAGS)
//				break;
			if(item.getNumTracks() == 0) {
				continue;
			}
			attrIndex = item.getPositionInResource();
			// check for no position
			// if there is no intrinsic position from resource XML we assign the current counter value
			if (attrIndex == MediaGroup.NO_POSITION)
				attrIndex = c++;
			if (attrDefs.size() > attrIndex) {
				Bubble bubble = new Bubble(org.apache.commons.lang3.text.WordUtils.capitalize(item.getName()), attrDefs.get(attrIndex).getCenter(), radiusForNumTracks(item.getNumTracks(),
						sumTracks, MAX_SIZE), attrDefs.get(attrIndex).getColor(), item, null);
				bubbles.add(bubble);
				createChildBubbles(item, bubble);
			} else {
				LOGGER.warn("No center and/or color definition for index {}", attrIndex);
			}
			//Log.v("MyLog",i+" -> bubble: "+bubble);
		}
		
		return bubbles;
	}

	
		
	
	public void createChildBubbles(MediaGroup<? extends MediaItem> group, Bubble parentBubble) {
		List<Bubble> childBubbles = Lists.newArrayList();
		for(MediaItem child : group.getChildren()) {
			if(child.shouldShowAsBubble() == false) {
				continue;
			}
			Bubble bubble = new Bubble(child.getName(), radiusForNumTracks(child.getNumTracks(), group.getNumTracks(),
					parentBubble.getRadius() * PARENT_CHILD_RADIUS_FACTOR), child, parentBubble);
			childBubbles.add(bubble);
			if(child instanceof MediaGroup) {
				MediaGroup<?> childGroup = (MediaGroup<?>) child;
				createChildBubbles(childGroup, bubble);
			}
		}
		layoutBubbles(childBubbles, parentBubble.getRadius());
		parentBubble.addChildren(childBubbles);
	}	

}
