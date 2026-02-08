<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>Super App Admin Login</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        body {
            background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); /* 고급스러운 블루 그라데이션 */
            height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: 'Pretendard', sans-serif;
        }
        .login-card {
            background: rgba(255, 255, 255, 0.95);
            padding: 40px;
            border-radius: 16px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
            width: 100%;
            max-width: 400px;
        }
        .login-title {
            font-weight: 800;
            color: #1e3c72;
            text-align: center;
            margin-bottom: 30px;
        }
        .form-control {
            height: 50px;
            border-radius: 8px;
        }
        .btn-login {
            height: 50px;
            background-color: #1e3c72;
            border: none;
            font-weight: bold;
            font-size: 16px;
            border-radius: 8px;
            transition: 0.3s;
        }
        .btn-login:hover {
            background-color: #162e58;
        }
    </style>
</head>
<body>

    <div class="login-card">
        <h2 class="login-title">스타플랫폼 로그인</h2>
        <form id="loginForm">
            <div class="mb-3">
                <label for="PRS_ID" class="form-label fw-bold">아이디</label>
                <input type="text" class="form-control" id="PRS_ID" name="PRS_ID" placeholder="Admin ID" required>
            </div>
            <div class="mb-4">
                <label for="PRS_PWD" class="form-label fw-bold">비밀번호</label>
                <input type="password" class="form-control" id="PRS_PWD" name="PRS_PWD" placeholder="Password" required>
            </div>
            <div class="d-grid">
                <button type="submit" class="btn btn-primary btn-login">로그인</button>
            </div>
        </form>
        <p class="text-center text-muted mt-3 small">
            &copy; Sensible Super App Platform
        </p>
    </div>

<script>
    $(document).ready(function() {
        $('#loginForm').on('submit', function(e) {
            e.preventDefault(); // 기본 제출 막기

            $.ajax({
                url: '/witch/super/loginPrc.do',
                type: 'POST',
                data: $(this).serialize(),
                success: function(res) {
                	if (res.status === 'success') {
                	    // 서버가 주는 주소로 이동 (없으면 기본 대시보드)
                	    location.href = res.redirectUrl ? res.redirectUrl : '/witch/super/dashboard.do';
                	} else {
                        alert(res.msg);
                    }
                },
                error: function(err) {
                    console.error(err);
                    alert('서버 통신 오류가 발생했습니다.');
                }
            });
        });
    });
</script>

</body>
</html>