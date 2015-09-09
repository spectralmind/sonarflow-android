package com.spectralmind.profiling;

public class MyTimer {

	public interface Callback
	{
		public void onTimerEnd();
	}
	
	private long millis = 0;
	private Callback callBack;
	
	public MyTimer(long millis)
	{
		this.millis = millis;
	}
	
	public void start(Callback c)
	{
		this.callBack = c;
		new Thread(new Runnable()
		{
			@Override
			public void run() {
				try {
					Thread.sleep(millis);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(callBack != null)
					callBack.onTimerEnd();
			}
			
		}).start();
	}
}
