package com.top1.client.island;


public class Anim {
	private float value;
	private float previous;
	private float from;
	private float target;
	private long start;
	private long duration;

	public Anim(long duration) {
		this.duration = duration;
		this.start = System.currentTimeMillis() - duration;
	}

	public void update(float target) {
		if(target != this.target){
			this.from = this.value;
			this.target = target;
			this.start = System.currentTimeMillis();
		}
		float t = Math.min(1.0F, (System.currentTimeMillis() - this.start) / (float) this.duration);
		t = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
		this.previous = this.value;
		this.value = this.from + (this.target - this.from) * t;
	}

	public float get() {
		return this.value;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public float delta() {
		return this.value - this.previous;
	}

	public void snap(float value) {
		this.value = value;
		this.from = value;
		this.target = value;
		this.start = System.currentTimeMillis() - this.duration;
	}
}
