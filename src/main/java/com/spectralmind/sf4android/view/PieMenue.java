package com.spectralmind.sf4android.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PieMenue extends FrameLayout {

	public interface OnClickedListener
	{
		public void onItemClicked(int itemNumber);
	}
	
	public class Item{
		public Item(float start,String name)
		{		
			if(start < -180)
				start = (start + 180) * -1;

			this.startAngle = start;
			this.itemImage = name;
		}
		private float startAngle;
		private String itemImage;
		private boolean locked = false;
		
		public void lock(boolean value)
		{
			locked = value;
		}
		
		public boolean isLocked()
		{
			return locked;
		}
		
		public float getStartAngle()
		{
			return startAngle;
		}
		public String getItemImage()
		{
			return itemImage;
		}
	}
	
	private OnClickedListener onItemClicked;
	private int width,height;
	private Point center;
	private float middleRadius;
	private String mainImage;
	private String middleImage;
	private ImageView main;
	private float deltaAngle;
	private boolean CREATED = false;
	private ArrayList<Item> items;
	int itemFocused = -1;
	private GestureDetector gestureDetector;
	private boolean locked = false;
	
	private int flag;
	
	public PieMenue(Context context) {
		this(context, null);
	}
	
	public PieMenue(Context context, AttributeSet attrs) {
	super(context, attrs);
	setDrawingCacheEnabled(true);
	}
	
	public void setFlag(int i)
	{
		this.flag = i;
	}
	
	public int getFlag()
	{
		return flag;
	}
	
	public void createPieMenue(int width,int height,int startAngle,String mainImage,String[] items)
	{
		if(items.length < 1 || CREATED)
			return;
		this.CREATED = true;
		this.width = width;
		this.height = height;
		this.center = new Point(width/2,height/2);
		this.middleRadius = width / 7;
		this.items = new ArrayList<Item>();
		this.mainImage = mainImage;
		this.middleImage = items[0];
		
		main = new ImageView(getContext());
		FrameLayout.LayoutParams params1 = new FrameLayout.LayoutParams(
				width,height);
		params1.leftMargin = 0;
		params1.topMargin = 0;
		
		main.setBackgroundResource(getResources().
				getIdentifier(mainImage,"drawable", getContext().getPackageName()));
		
//		main.setBackgroundColor(Color.CYAN);

		addView(main,params1);

		deltaAngle = 360 / (items.length - 1);
		for(int i = 1;i<items.length;i++)
		{
			this.items.add(new Item(startAngle-deltaAngle*(i-1),items[i]));
		}
		
		gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				e.getDownTime();
				handleTap(e.getX(),e.getY());
				return true;
			}
		});
	}	
	
	public void setOnItemClickedListener(OnClickedListener o)
	{
		this.onItemClicked = o;
	}
	
	private void handleTap(float x, float y) {
		int i = askMenuItem(x,y);
		if(i == 0)
			onItemClicked.onItemClicked(0);
		else
			onItemClicked.onItemClicked(i);
	}
	
	private boolean ifNotTransparent(int x,int y)
	{
		int color = getDrawingCache().getPixel((int)x,(int)y);
		if(color == 0)
		{
			main.setBackgroundResource(getResources().
					getIdentifier(mainImage,"drawable", getContext().getPackageName()));
			this.itemFocused = -1;
			return true;
		}
		return false;
	}
	
	private void askLostFocused()
	{
		if(itemFocused == -1)
			return;
		else
		{
			if(onItemClicked != null)
				onItemClicked.onItemClicked(itemFocused);
		}
	}
	
	public void lockItem(int i)
	{
		if(items == null)
			return;
		if(i - 1 > items.size() - 1)
			return;
		items.get(i-1).lock(true);
	}
	
	public boolean onTouchEvent(MotionEvent event)
	{
		if(locked)
			return true;
		if(event.getPointerCount() > 1)
			return true;
		
		int x = (int)event.getX();
		int y = (int)event.getY();
		if(x < 1 || x >= width || y < 1 || y >= height)
		{
			main.setBackgroundResource(getResources().
					getIdentifier(mainImage,"drawable", getContext().getPackageName()));
			return true;
		}
		
//		gestureDetector.onTouchEvent(event);
		int action = event.getActionMasked();
		
		if(action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP)
		{
			main.setBackgroundResource(getResources().
					getIdentifier(mainImage,"drawable", getContext().getPackageName()));
			askLostFocused();
			return true;
		}
		
		if(ifNotTransparent(x,y))
			return true;
		
		int i = askMenuItem(x,y);
		if(i == 0)
		{
			main.setBackgroundResource(getResources().getIdentifier(middleImage,
							"drawable", getContext().getPackageName()));
			this.itemFocused = 0;
		}
		else
		{
			if(!items.get(i-1).isLocked())
			{
			main.setBackgroundResource(getResources().
			getIdentifier(items.get(i-1).getItemImage(),
					"drawable", getContext().getPackageName()));
			this.itemFocused = i;
			}
		}
		return true;
	}
	
	private int askMenuItem(float x,float y)
	{		
		y = height - y;
		x = x - center.x;
		y = y - center.y;
		
		if(Math.sqrt(x*x+y*y) < middleRadius)
			return 0;
		
		float degree = (float) (Math.atan2(y, x) * 180 / Math.PI);
		
		for(int i = 0; i < items.size(); i++)
		{
			if(degree < items.get(i).getStartAngle() && 
					degree > items.get(i+1).getStartAngle())
			{
				return i + 1;
			}
			else if(i == items.size() - 2)
			{
				return i + 2;
			}
		}
		return -1;
	}

	public void lockTouch() {
		this.locked = true;
	}
}
