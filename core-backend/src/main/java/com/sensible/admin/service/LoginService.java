package com.sensible.admin.service;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.sensible.admin.domain.UserVO;
import com.sensible.common.dao.DefaultDAO;

@Service("loginService")
public class LoginService {

	
	@Resource(name="DefaultDAO")
    private DefaultDAO dao;
	
	public UserVO selectLogin(UserVO paramVO) throws Exception
	{
		return dao.selectOne("login.selectLogin", paramVO);
	}

	
	public void setNewPassword(Map<String, Object> map) throws Exception {
		
		dao.update("admin.pressUpdate", map);
		
	}
}
