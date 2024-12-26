package mypage.model;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import member.domain.MemberVO;
import util.security.AES256;
import util.security.SecretMyKey;
import util.security.Sha256;

public class MypageDAO_imple implements MypageDAO {
	
	private DataSource ds;	// DataSource ds 는 아파치톰캣이 제공하는 DBCP(DB Connection Pool)이다.
	private Connection conn;
	private PreparedStatement pstmt;
	private ResultSet rs;
	
	private AES256 aes;

	// 생성자
	public MypageDAO_imple() {
		try {
			Context initContext = new InitialContext();
			Context envContext  = (Context)initContext.lookup("java:/comp/env");
			ds = (DataSource)envContext.lookup("jdbc/semioracle");
			
			aes = new AES256(SecretMyKey.KEY);
			// SecretMyKey.KEY 은 우리가 만든 암호화/복호화 키이다.
			
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	} // end of public ProductDAO_imple()--------------------------------
	
	// 사용한 자원을 반납하는 close() 메소드 생성하기
	private void close() {
		try {
			if(rs != null) {rs.close(); rs = null;}
			if(pstmt != null) {pstmt.close(); pstmt = null;}
			if(conn != null) {conn.close(); conn = null;}
		} catch(SQLException e) {
			e.printStackTrace();
		}
	} // end of private void close()------------------------------------

	@Override
	public boolean checkPassword(Map<String, String> paraMap) throws SQLException {
		boolean checkPassword = false;

		try {
			conn = ds.getConnection();

			String sql = " select pwd " + " from tbl_member " + " where user_id = ? and pwd = ? ";

			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, paraMap.get("userid"));
			pstmt.setString(2, Sha256.encrypt(paraMap.get("pwd")));

			rs = pstmt.executeQuery();
			checkPassword = rs.next();
		        
		} finally {
			close();
		}
		return checkPassword;
	}

	@Override
	public boolean emailDuplicateCheck2(Map<String, String> paraMap) throws SQLException {
		boolean isExists = false;

		try {
			conn = ds.getConnection();

			String sql = " select email " + " from tbl_member " + " where user_id = ? and email = ? ";

			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, paraMap.get("userid"));
			pstmt.setString(2, aes.encrypt(paraMap.get("email")));

			rs = pstmt.executeQuery();

			isExists = rs.next();

		} catch (GeneralSecurityException | UnsupportedEncodingException e) {
		} finally {
			close();
		}

		return isExists;
	}

	//회원정보수정
	@Override
	public int updateMember(MemberVO member) throws SQLException {
		int result = 0;

		try {
			conn = ds.getConnection();
			
			String sql = " update tbl_member set name = ? "
	                  + "                     , email = ? "
	                  + "                     , mobile = ? "
	                  + " where user_id = ? ";
			
			pstmt = conn.prepareStatement(sql);
	         
	         pstmt.setString(1, member.getName());
	         pstmt.setString(2, aes.encrypt(member.getEmail()) );  // 이메일을 AES256 알고리즘으로 양방향 암호화 시킨다. 
	         pstmt.setString(3, aes.encrypt(member.getMobile()) ); // 휴대폰번호를 AES256 알고리즘으로 양방향 암호화 시킨다. 
	         pstmt.setString(4, member.getUserid());
	         
	         result =  pstmt.executeUpdate();
	         
		} catch (GeneralSecurityException | UnsupportedEncodingException e) {
		}finally {
			close();
		}
		
		
		return result;
	}

	@Override
	public int mypwdUdate(Map<String, String> paraMap) throws SQLException {
		int result = 0;

		try {
			conn = ds.getConnection();

			String sql = " update tbl_member set pwd = ?, PWD_CHANGE_DATE = sysdate " + " where user_id = ? ";

			pstmt = conn.prepareStatement(sql);

			pstmt.setString(1, Sha256.encrypt(paraMap.get("new_pwd"))); // 암호를 SHA256 알고리즘으로 단방향 암호화 시킨다.
			pstmt.setString(2, paraMap.get("userid"));

			result = pstmt.executeUpdate();

		} finally {
			close();
		}

		return result;
	}

	
	
	
	
	
	
	
	
	
	
	
	

}//end of public class MypageDAO_imple implements MypageDAO-----
