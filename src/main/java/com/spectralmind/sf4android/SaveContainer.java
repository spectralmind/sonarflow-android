package com.spectralmind.sf4android;

import java.util.ArrayList;

import android.app.Fragment;

import com.spectralmind.sf4android.bubble.Animator.AnimationHistory;
import com.spectralmind.sf4android.bubble.BubblesView.PiemenueSaver;

public class SaveContainer extends Fragment {
	
	private SonarflowState state;
	private PiemenueSaver pieSave;
	private ArrayList<AnimationHistory> animationList;
	
	public ArrayList<AnimationHistory> getAnimationList() {
		return animationList;
	}

	public void setAnimationList(ArrayList<AnimationHistory> animationList) {
		this.animationList = animationList;
	}

	public void saveState(SonarflowState state)
	{
		this.state = state;
	}
	
	public SonarflowState getState()
	{
		return this.state;
	}
	
	public void savePiemenue(PiemenueSaver save)
	{
		pieSave = save;
	}
	
	public PiemenueSaver getPiemenueSaver()
	{
		PiemenueSaver buff = pieSave;
		pieSave = null;
		return buff;
	}
}
