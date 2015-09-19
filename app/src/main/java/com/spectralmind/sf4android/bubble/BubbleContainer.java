package com.spectralmind.sf4android.bubble;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.graphics.PointF;

import com.google.common.collect.Lists;

public class BubbleContainer implements Iterable<Bubble> {
	private List<Bubble> bubbles = Collections.synchronizedList(
			new CopyOnWriteArrayList<Bubble>());
	
//	private final List<Bubble> bubbles = new CopyOnWriteArrayList<Bubble>();
	
	public void clearAll() {
		removeAllDiscoveryBubbles();
		bubbles = Collections.synchronizedList(new CopyOnWriteArrayList<Bubble>());
	}
	
	@Override
	public Iterator<Bubble> iterator() {
		return bubbles.iterator();
	}

	public void addAll(List<Bubble> bubbles) {
		if(bubbles == null) //sometimes this variable is null ToDo: find out why
			return;
		this.bubbles.addAll(bubbles);
	}
	
	public void add(Bubble bubble)
	{
		if(bubbles == null) //sometimes this variable is null ToDo: find out why
			return;
		this.bubbles.add(bubble);
	}

	public void removeLastBubble()
	{
		if(!bubbles.isEmpty())
			bubbles.remove(bubbles.size()-1);
	}
	
	public int getSize()
	{
		return bubbles.size();
	}
	
	public Bubble getLastBubble()
	{
		if(bubbles.isEmpty())
			return null;
		return bubbles.get(bubbles.size()-1);
	}
	
	public synchronized List<Bubble> getBubbles(int start,int end)
	{
		return bubbles.subList(start, end);
	}
	
	public Bubble bubbleForLocation(PointF location, Converter converter) {
		Bubble result = null;
		for(Bubble bubble : bubbles) {
			Bubble candidate = bubble.bubbleForLocation(location, converter);
			if(candidate == null) {
				continue;
			}
			if(result == null || candidate.getRadius() < result.getRadius()) {
				result = candidate;
			}
		}
		return result;
	}

	public boolean isEmpty() {
		return bubbles.isEmpty();
	}

	public void removeAllDiscoveryBubbles() {
		bubbles.removeAll(getDiscoveryBubbles());
	}

	public synchronized List<Bubble> getDiscoveryBubbles() {
		if (getSize() == 0)
			return Lists.newArrayList();		
		int i = 0;
		while(bubbles.get(getSize()-i-1).type == DiscoveryBubble.DISCOVERY_BUBBLE)
		{
			i++;
		}
		if(i == 0)
			return Lists.newArrayList();
		else{
			return getBubbles(getSize()-i,getSize());
		}
	}

	public void refreshAllDiscoveryBubbles() {
		if(bubbles.size() == 0)
			return;
		List<Bubble> remove = new CopyOnWriteArrayList<Bubble>();
		int i = 0;
		while(bubbles.get(getSize()-i-1).type == DiscoveryBubble.DISCOVERY_BUBBLE)
		{
			if(!bubbles.get(getSize()-i-1).isFocused()){
				remove.add(bubbles.get(getSize()-i-1));}
			else{
				bubbles.get(getSize()-i-1).setFocused(false);}
			i++;
		}
		bubbles.removeAll(remove);	
	}
}
