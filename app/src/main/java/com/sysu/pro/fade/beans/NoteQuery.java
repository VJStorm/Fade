package com.sysu.pro.fade.beans;

import java.util.List;

public class NoteQuery {
	private Integer start; // 发给服务器的：向下加载查询的起点，一开始填0
	private List<Note> list;// 查询得到的新数据,最多十条
	private List<Note> updateList;// 已加载的帖子 再 经过服务器查询筛选出来的剩余贴子

	private String point;//通知页面分段查询用到的,记录时间点

	private Integer sum;

	private String queryKeyWord;

	public String getQueryKeyWord() {
		return queryKeyWord;
	}

	public void setQueryKeyWord(String queryKeyWord) {
		this.queryKeyWord = queryKeyWord;
	}

	public Integer getSum() {
		return sum;
	}

	public void setSum(Integer sum) {
		this.sum = sum;
	}

	public Integer getStart() {
		return start;
	}

	public void setStart(Integer start) {
		this.start = start;
	}

	public List<Note> getList() {
		return list;
	}

	public void setList(List<Note> list) {
		this.list = list;
	}

	public List<Note> getUpdateList() {
		return updateList;
	}

	public void setUpdateList(List<Note> updateList) {
		this.updateList = updateList;
	}

	public String getPoint() {
		return point;
	}

	public void setPoint(String point) {
		this.point = point;
	}
}
