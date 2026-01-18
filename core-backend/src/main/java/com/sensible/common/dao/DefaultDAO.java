package com.sensible.common.dao;

import jbit.core.dao.AbstractCommonDAO;

import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository("DefaultDAO")
public class DefaultDAO extends AbstractCommonDAO{

	private DefaultDAO()
	{
		super();
	}
}
