package com.sensible.api.controller;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sensible.api.service.ContentService;
import com.sensible.api.service.MemberService;
import com.sensible.common.Constants;
import com.sensible.common.dao.DefaultDAO;
import com.sensible.common.domain.CommandMap;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import sun.security.ec.ECPrivateKeyImpl;

import java.io.FileReader;
import java.io.IOException;
import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.security.InvalidKeyException;
import java.security.interfaces.ECPrivateKey;


@Controller
public class AppleController {
	
	/**
	 *  추후 해당 내용을 properties에 옮기시는것을 추천드립니다.
	 */

    @javax.annotation.Resource(name = "memberService")
    private MemberService memberService;

    @javax.annotation.Resource(name = "contentService")
    private ContentService contentService;

    @javax.annotation.Resource(name="DefaultDAO")
    private DefaultDAO dao;

    @javax.annotation.Resource(name = "bcryptPasswordEncoder")
    private BCryptPasswordEncoder bcryptPasswordEncoder;

	public static final String TEAM_ID = "VU2D7F2P7E";
	public static final String REDIRECT_URL = "https://witch-hunting.com/callback/loginCallBackApple";
	public static final String CLIENT_ID = "kr.co.sensiblenews.login";
	public static final String KEY_ID = "5UVLSJZ7AL";
	
	/* <!-- 고정 --> */
	public static final String AUTH_URL = "https://appleid.apple.com";
	/* <!-- 앞전에 다운받은 AuthKey.p8 경로 --> */
	private String KEY_PATH = "apple/AuthKey_5UVLSJZ7AL.p8";
	
	/**
	 * Controller
	 */


	@RequestMapping(value = "/appleRestLogin", method = RequestMethod.GET)
	public String kakaoRestLogin(Model model, Locale locale) {
        System.out.println("[CTSOFT] KEY_PATH : " + KEY_PATH);
        return "login/apple";

	}
	
	@RequestMapping(value = "/login/getAppleAuthUrl")
	public @ResponseBody String getAppleAuthUrl(
			HttpServletRequest request) throws Exception {
		
	    String reqUrl = 
	    		AUTH_URL 
	    		+ "/auth/authorize?client_id=" 
	    	    + CLIENT_ID
	    	    + "&redirect_uri=" 
	    	    + REDIRECT_URL
	            + "&response_type=code id_token&scope=name email&response_mode=form_post";

	    return reqUrl;
	}

	@RequestMapping(value = "/callback/loginCallBackApple", method = RequestMethod.POST)
	public String oauth_apple(
            CommandMap commandMap,
			HttpServletRequest request
			, @RequestParam(value = "code", required= false) String code
			, HttpServletResponse response
			, Model model) throws Exception {
		
		// 애플로그인 취소시 로그인페이지로 이동
		if(code == null) {
			return "login/apple";
		}
	    String client_id = CLIENT_ID;
	    String client_secret = createClientSecret(TEAM_ID, CLIENT_ID, KEY_ID, KEY_PATH, AUTH_URL);
	    // 토큰 검증 및 발급
	    String reqUrl = AUTH_URL + "/auth/token";
	    Map<String, String> tokenRequest = new HashMap<>();
	    tokenRequest.put("client_id", client_id);
	    tokenRequest.put("client_secret", client_secret);
	    tokenRequest.put("code", code);
	    tokenRequest.put("grant_type", "authorization_code");
	    String apiResponse = doPost(reqUrl, tokenRequest);
	    
	    JSONObject tokenResponse = new JSONObject(apiResponse);
        JSONObject appleInfo = decodeFromIdToken(tokenResponse.getString("id_token"));
        
        /**
        TO DO : 리턴받은 appleInfo로 
        		, 회원가입처리
        		, 로그인처리
        		, 처리 후 원하는 위치 이동
        */


        Map<String, Object> map = new HashMap<String, Object>();

        String ktKey = appleInfo.getString("sub");

        String token = memberTokenEncrypt("AP",ktKey);
        String ktToken = tokenResponse.getString("access_token");

        map.put("MEM_TOKEN", ktKey);
        map.put("MEM_TYPE", "AP");
        map.put("APP_ID", "1");
        map.put("MEM_NAME", appleInfo.getString("email"));
        map.put("AP_KEY", ktKey);
        map.put("AP_CONTENT", tokenResponse.toString());
        map.put("AP_TOKEN", ktKey);
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, Object> kkcMap = dao.selectOne("member.apKeyCheck", ktKey);
        if (kkcMap != null && kkcMap.get("MEM_ID") != null) {
            String memId = (String) kkcMap.get("MEM_ID");
            map.put("MEM_ID", memId);
            dao.update("member.updateMember", map);
            resultMap = dao.selectOne("member.selectMember", memId);
        } else {
            String memId = dao.selectOne("member.nextMemId");
            map.put("MEM_ID", memId);
            dao.insert("member.insertMember", map);
            dao.insert("member.insertApple", map);
            resultMap = dao.selectOne("member.selectMember", memId);
        }
        // 추후 아래는 삭제
        model.addAttribute("appleInfo", appleInfo);
        model.addAttribute("resultMap", resultMap);

        return "login/apple_success";
	}
	////////////////////////////////////////
	
	
	/**
	 * Util
	 */
	
    public String createClientSecret(String teamId, String clientId, String keyId, String keyPath, String authUrl) {

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build();
        JWTClaimsSet claimsSet = new JWTClaimsSet();
        Date now = new Date();

        claimsSet.setIssuer(teamId);
        claimsSet.setIssueTime(now);
        claimsSet.setExpirationTime(new Date(now.getTime() + 3600000));
        claimsSet.setAudience(authUrl);
        claimsSet.setSubject(clientId);

        SignedJWT jwt = new SignedJWT(header, claimsSet);

        // try {
        //     ECPrivateKey ecPrivateKey = new ECPrivateKeyImpl(readPrivateKey(keyPath));
        //     JWSSigner jwsSigner = new ECDSASigner(ecPrivateKey.getS());

        //     jwt.sign(jwsSigner);

        // } catch (InvalidKeyException e) {
        //     e.printStackTrace();
        // } catch (JOSEException e) {
        //     e.printStackTrace();
        // }

        return jwt.serialize();
    }   
    
    private byte[] readPrivateKey(String keyPath) {

        Resource resource = new ClassPathResource(keyPath);
        byte[] content = null;

        try (FileReader keyReader = new FileReader(resource.getFile());
             PemReader pemReader = new PemReader(keyReader)) {
            {
                PemObject pemObject = pemReader.readPemObject();
                content = pemObject.getContent();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }
    
    public String doPost(String url, Map<String, String> param) {
        System.out.println("[실행] doPost : "+url);
        String result = null;
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        Integer statusCode = null;
        try {
            httpclient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
            List<NameValuePair> nvps = new ArrayList<>();
            Set<Map.Entry<String, String>> entrySet = param.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();
                nvps.add(new BasicNameValuePair(fieldName, fieldValue));
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nvps);
            httpPost.setEntity(formEntity);
            response = httpclient.execute(httpPost);
            statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, "UTF-8");

            if (statusCode != 200) {
                System.out.println("애러");
            }
            EntityUtils.consume(entity);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                if (httpclient != null) {
                    httpclient.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("[결과] doPost : "+result);
        return result;
    }
    

    public JSONObject decodeFromIdToken(String id_token) {

        try {
            SignedJWT signedJWT = SignedJWT.parse(id_token);
            ReadOnlyJWTClaimsSet getPayload = signedJWT.getJWTClaimsSet();
    	    String appleInfo = getPayload.toJSONObject().toJSONString();
    	    JSONObject payload = new JSONObject(appleInfo);

            if (payload != null) {
                return payload;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void sessionInfoGetAndPut(CommandMap commandMap, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String appId = String.valueOf(session.getAttribute(Constants._APP_SESSION_KEY));
        String memId = String.valueOf(session.getAttribute("MEM_ID"));
        //String memId = "2024012700001";
        System.out.println("[APP_ID] ::::::::::::::::::: "+appId);
        System.out.println("[MEM_ID] ::::::::::::::::::: "+memId);

        commandMap.put("APP_ID", appId);
        commandMap.put("MEM_ID", memId);
    }

    public String memberTokenEncrypt(String type, String Key) {
        //long now = System.currentTimeMillis();
        String str = "sEnSiBle";
        //String nowStr = Long.toString(now);
        String sign = bcryptPasswordEncoder.encode(type + Key + str);
        return sign;
    }

}