import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/useAuth";
import axios from "axios";
import { setAccessToken } from "../../lib/token";
import { API_BASE_URL } from "../../config/apiBase";

const API = API_BASE_URL;
const SERVER = API_BASE_URL;

export default function LoginPage({ success }: { success?: boolean }) {
  const { search } = useLocation();
  const params = new URLSearchParams(search);
  const redirect = params.get("redirect") || "/";
  const navigate = useNavigate();
  const { refresh, user, loading, setUser } = useAuth();

  // 로컬 로그인 상태
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  // 모달 상태
  const [showFindIdModal, setShowFindIdModal] = useState(false);
  const [showResetPasswordModal, setShowResetPasswordModal] = useState(false);

  // 아이디 찾기 상태
  const [findIdName, setFindIdName] = useState("");
  const [findIdPhone, setFindIdPhone] = useState("");
  const [findIdCode, setFindIdCode] = useState("");
  const [findIdCodeSent, setFindIdCodeSent] = useState(false);
  const [findIdVerified, setFindIdVerified] = useState(false);
  const [findIdError, setFindIdError] = useState("");
  const [findIdLoading, setFindIdLoading] = useState(false);
  const [foundEmail, setFoundEmail] = useState("");
  const [foundAuthProvider, setFoundAuthProvider] = useState("");

  // 비밀번호 찾기 상태
  const [resetEmail, setResetEmail] = useState("");
  const [resetPhone, setResetPhone] = useState("");
  const [resetCode, setResetCode] = useState("");
  const [resetNewPassword, setResetNewPassword] = useState("");
  const [resetCodeSent, setResetCodeSent] = useState(false);
  const [resetVerified, setResetVerified] = useState(false);
  const [resetError, setResetError] = useState("");
  const [resetLoading, setResetLoading] = useState(false);

  // OAuth access 토큰 교환 및 유저 정보 갱신
  useEffect(() => {
    if (!success) return;
    (async () => {
      try {
        const { data } = await axios.post(`${API}/api/token`, {}, { withCredentials: true });
        setAccessToken(data.accessToken);
        await refresh();
      } catch (e) {
        console.error(e);
      }
    })();
  }, [success, refresh]);

  // user가 갱신되면 리다이렉트 (OAuth 성공 시에만)
  useEffect(() => {
    console.log("user정보", user);
    if (success && user && !loading) {
      navigate(redirect, { replace: true });
    }
  }, [user, loading, success, navigate, redirect]);

  // 이미 로그인된 사용자는 리다이렉트 (일반 접근 시)
  useEffect(() => {
    if (!success && user && !loading) {
      navigate(redirect, { replace: true });
    }
  }, [user, loading, success, navigate, redirect]);

  // 로컬 로그인 처리
  const handleLocalLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      const response = await axios.post(`${API}/api/auth/login`, {
        email,
        password,
      });

      if (response.data.accessToken) {
        setAccessToken(response.data.accessToken);
        
        // 백엔드에서 사용자 정보도 함께 받아서 직접 설정
        if (response.data.user) {
          setUser(response.data.user);
        }
        
        navigate(redirect, { replace: true });
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "로그인에 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  // 아이디 찾기 관련 함수들
  const handleSendCodeForFindId = async () => {
    setFindIdError("");
    if (!findIdPhone) return setFindIdError("전화번호를 입력하세요.");
    setFindIdLoading(true);
    try {
      await axios.post(`${API}/api/phone/send`, { phoneNumber: findIdPhone });
      setFindIdCodeSent(true);
    } catch (err: any) {
      setFindIdError(err.response?.data?.error || "코드 전송에 실패했습니다.");
    } finally {
      setFindIdLoading(false);
    }
  };

  const handleVerifyCodeForFindId = async () => {
    setFindIdError("");
    if (!findIdCode) return setFindIdError("인증번호를 입력하세요.");
    setFindIdLoading(true);
    try {
      await axios.post(`${API}/api/phone/verify`, { phoneNumber: findIdPhone, code: findIdCode });
      setFindIdVerified(true);
    } catch (err: any) {
      setFindIdError(err.response?.data?.error || "인증번호가 올바르지 않습니다.");
    } finally {
      setFindIdLoading(false);
    }
  };

  const handleFindId = async () => {
    setFindIdError("");
    if (!findIdName || !findIdPhone || !findIdVerified) return setFindIdError("모든 필드를 입력하고 인증하세요.");
    setFindIdLoading(true);
    try {
      const response = await axios.post(`${API}/api/auth/find-id`, { name: findIdName, phone: findIdPhone });
      setFoundEmail(response.data.email);
      setFoundAuthProvider(response.data.authProvider || "");
    } catch (err: any) {
      setFindIdError(err.response?.data?.error || "아이디 찾기에 실패했습니다.");
    } finally {
      setFindIdLoading(false);
    }
  };

  // 비밀번호 찾기 관련 함수들
  const handleSendCodeForReset = async () => {
    setResetError("");
    if (!resetPhone) return setResetError("전화번호를 입력하세요.");
    setResetLoading(true);
    try {
      await axios.post(`${API}/api/phone/send`, { phoneNumber: resetPhone });
      setResetCodeSent(true);
    } catch (err: any) {
      setResetError(err.response?.data?.error || "코드 전송에 실패했습니다.");
    } finally {
      setResetLoading(false);
    }
  };

  const handleVerifyCodeForReset = async () => {
    setResetError("");
    if (!resetCode) return setResetError("인증번호를 입력하세요.");
    setResetLoading(true);
    try {
      await axios.post(`${API}/api/phone/verify`, { phoneNumber: resetPhone, code: resetCode });
      setResetVerified(true);
    } catch (err: any) {
      setResetError(err.response?.data?.error || "인증번호가 올바르지 않습니다.");
    } finally {
      setResetLoading(false);
    }
  };

  const handleResetPassword = async () => {
    setResetError("");
    if (!resetEmail || !resetPhone || !resetNewPassword || !resetVerified) return setResetError("모든 필드를 입력하고 인증하세요.");
    setResetLoading(true);
    try {
      await axios.post(`${API}/api/auth/reset-password`, { email: resetEmail, phone: resetPhone, newPassword: resetNewPassword });
      alert("비밀번호가 재설정되었습니다.");
      setShowResetPasswordModal(false);
    } catch (err: any) {
      setResetError(err.response?.data?.error || "비밀번호 재설정에 실패했습니다.");
    } finally {
      setResetLoading(false);
    }
  };

  return (
    <section className="bg-[#FFFFF0] w-full relative">
      {/* 배경 이미지 */}
      <div
        className="absolute inset-0 w-full h-screen bg-cover bg-center"
        style={{
          backgroundImage: `url('${SERVER}/images/emptyload.webp')`,
          zIndex: 0,
        }}
      />
      
      {/* 콘텐츠 영역 */}
      <div
        className="absolute inset-0 flex items-center justify-center px-4"
        style={{ zIndex: 5 }}
      >
        {/* 가장 바깥 테두리 (#000) */}
        <div
          className="relative bg-[#000] mt-6 mx-3 md:mx-0"
          style={{
            imageRendering: 'pixelated',
            clipPath: `polygon(
              0% 20px, 20px 20px, 20px 0%,
              calc(100% - 20px) 0%, calc(100% - 20px) 20px, 100% 20px,
              100% calc(100% - 20px), calc(100% - 20px) calc(100% - 20px), calc(100% - 20px) 100%,
              20px 100%, 20px calc(100% - 20px), 0% calc(100% - 20px)
            )`,
            padding: '12px'
          }}
        >
          {/* 중간 테두리 (#1a5f7a) */}
          <div
            className="relative bg-[#1a5f7a]"
            style={{
              imageRendering: 'pixelated',
              clipPath: `polygon(
                0% 18px, 18px 18px, 18px 0%,
                calc(100% - 18px) 0%, calc(100% - 18px) 18px, 100% 18px,
                100% calc(100% - 18px), calc(100% - 18px) calc(100% - 18px), calc(100% - 18px) 100%,
                18px 100%, 18px calc(100% - 18px), 0% calc(100% - 18px)
              )`,
              padding: '24px'
            }}
          >
            {/* 가장 안쪽 컨텐츠 (#F5DEB3) - 모바일 우선: max-width 적용 */}
            <div
              className="w-full px-6 py-5 bg-[#F5DEB3] max-w-md md:min-w-[400px] mx-auto"
              style={{
                imageRendering: 'pixelated',
                clipPath: `polygon(
                  0% 16px, 16px 16px, 16px 0%,
                  calc(100% - 16px) 0%, calc(100% - 16px) 16px, 100% 16px,
                  100% calc(100% - 16px), calc(100% - 16px) calc(100% - 16px), calc(100% - 16px) 100%,
                  16px 100%, 16px calc(100% - 16px), 0% calc(100% - 16px)
                )`
              }}
            >
          <h1 className="text-2xl md:text-3xl font-semibold mb-6 text-center" style={{ imageRendering: 'pixelated' }}>Sign in</h1>
          
          {/* 로컬 로그인 폼 */}
          <form onSubmit={handleLocalLogin} className="space-y-4 mb-6">
            <div>
              <label className="block text-sm font-medium mb-1">이메일</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-light"
                style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                placeholder="아이디를 입력하세요"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">비밀번호</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-light"
                style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                placeholder="비밀번호를 입력하세요"
                required
              />
            </div>
            {error && (
              <div className="text-red-500 text-sm">{error}</div>
            )}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full h-12 md:h-11 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center text-base"
            >
              {isLoading ? "로그인 중..." : "로그인"}
            </button>
          </form>

          {/* 구분선 */}
          <div className="relative mb-6">
            <div className="absolute inset-0 flex items-center">
              
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 text-gray-500">또는</span>
            </div>
          </div>

          {/* OAuth 로그인 */}
            <div className="space-y-3">
              <a
                href={`${API}/oauth2/authorization/kakao`}
                className="block h-12 rounded-md bg-[#FEE500] text-black font-medium flex items-center justify-center text-base oauth-unified"
              >
                Continue with Kakao
              </a>
              <a
                href={`${API}/oauth2/authorization/naver`}
                className="block h-12 rounded-md bg-[#03C75A] text-white font-medium flex items-center justify-center text-base oauth-unified"
              >
                Continue with Naver
              </a>
              <a
                href={`${API}/oauth2/authorization/google`}
                className="block h-12 rounded-md bg-slate-900 text-white font-medium flex items-center justify-center text-base oauth-unified"
              >
                Continue with Google
              </a>
            </div>

          {/* 회원가입 링크 */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600">
              계정이 없으신가요?{" "}
              <Link 
                to={`/signup?redirect=${encodeURIComponent(redirect)}`}
                replace={true}
                className="text-blue-600 hover:text-blue-500 font-medium"
              >
                회원가입
              </Link>
            </p>
            <p className="text-sm text-gray-600 mt-2">
              <button 
                onClick={() => setShowFindIdModal(true)}
                className="text-blue-600 hover:text-blue-500 font-medium mr-4"
              >
                아이디 찾기
              </button>
              |
              <button 
                onClick={() => setShowResetPasswordModal(true)}
                className="text-blue-600 hover:text-blue-500 font-medium ml-4"
              >
                비밀번호 찾기
              </button>
            </p>
          </div>
            </div>
          </div>
        </div>
      </div>
      
      {/* 레이아웃 높이 확보 */}
      <div style={{ height: "100vh" }} />

      {/* 아이디 찾기 모달 */}
      {showFindIdModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg max-w-md w-full mx-4">
            <h2 className="text-xl font-semibold mb-4" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>아이디 찾기</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>이름</label>
                <input
                  type="text"
                  value={findIdName}
                  onChange={(e) => setFindIdName(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                  placeholder="이름을 입력하세요"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>전화번호</label>
                <input
                  type="text"
                  value={findIdPhone}
                  onChange={(e) => setFindIdPhone(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                  placeholder="전화번호를 입력하세요"
                />
                <button
                  onClick={handleSendCodeForFindId}
                  disabled={findIdLoading || findIdCodeSent}
                  className="mt-2 text-sm text-blue-600 hover:text-blue-500 disabled:opacity-50"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                >
                  {findIdCodeSent ? "코드 전송됨" : "인증번호 전송"}
                </button>
              </div>
              {findIdCodeSent && (
                <div>
                  <label className="block text-sm font-medium mb-1" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>인증번호</label>
                  <input
                    type="text"
                    value={findIdCode}
                    onChange={(e) => setFindIdCode(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                    placeholder="인증번호를 입력하세요"
                  />
                  <button
                    onClick={handleVerifyCodeForFindId}
                    disabled={findIdLoading || findIdVerified}
                    className="mt-2 text-sm text-blue-600 hover:text-blue-500 disabled:opacity-50"
                    style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                  >
                    {findIdVerified ? "인증됨" : "인증 확인"}
                  </button>
                </div>
              )}
              {foundEmail && (
                <div className="text-green-600 text-sm" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>
                  <div className="mb-2">찾은 이메일: {foundEmail}</div>
                  {foundAuthProvider && (
                    <div className="text-blue-600">로그인 방식: {foundAuthProvider}</div>
                  )}
                </div>
              )}
              {findIdError && (
                <div className="text-red-500 text-sm" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>{findIdError}</div>
              )}
              <div className="flex space-x-2">
                <button
                  onClick={handleFindId}
                  disabled={findIdLoading || !findIdVerified}
                  className="flex-1 bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 disabled:opacity-50"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                >
                  {findIdLoading ? "찾는 중..." : "아이디 찾기"}
                </button>
                <button
                  onClick={() => {
                    setShowFindIdModal(false);
                    setFindIdName("");
                    setFindIdPhone("");
                    setFindIdCode("");
                    setFindIdCodeSent(false);
                    setFindIdVerified(false);
                    setFindIdError("");
                    setFoundEmail("");
                    setFoundAuthProvider("");
                  }}
                  className="flex-1 bg-gray-500 text-white py-2 rounded-md hover:bg-gray-600"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                >
                  닫기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 비밀번호 찾기 모달 */}
      {showResetPasswordModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg max-w-md w-full mx-4">
            <h2 className="text-xl font-semibold mb-4" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>비밀번호 재설정</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>이메일</label>
                <input
                  type="email"
                  value={resetEmail}
                  onChange={(e) => setResetEmail(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                  placeholder="이메일을 입력하세요"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>전화번호</label>
                <input
                  type="text"
                  value={resetPhone}
                  onChange={(e) => setResetPhone(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                  placeholder="전화번호를 입력하세요"
                />
                <button
                  onClick={handleSendCodeForReset}
                  disabled={resetLoading || resetCodeSent}
                  className="mt-2 text-sm text-blue-600 hover:text-blue-500 disabled:opacity-50"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                >
                  {resetCodeSent ? "코드 전송됨" : "인증번호 전송"}
                </button>
              </div>
              {resetCodeSent && (
                <div>
                  <label className="block text-sm font-medium mb-1" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>인증번호</label>
                  <input
                    type="text"
                    value={resetCode}
                    onChange={(e) => setResetCode(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                    placeholder="인증번호를 입력하세요"
                  />
                  <button
                    onClick={handleVerifyCodeForReset}
                    disabled={resetLoading || resetVerified}
                    className="mt-2 text-sm text-blue-600 hover:text-blue-500 disabled:opacity-50"
                    style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                  >
                    {resetVerified ? "인증됨" : "인증 확인"}
                  </button>
                </div>
              )}
              <div>
                <label className="block text-sm font-medium mb-1" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>새 비밀번호</label>
                <input
                  type="password"
                  value={resetNewPassword}
                  onChange={(e) => setResetNewPassword(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                  placeholder="새 비밀번호를 입력하세요"
                />
              </div>
              {resetError && (
                <div className="text-red-500 text-sm" style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}>{resetError}</div>
              )}
              <div className="flex space-x-2">
                <button
                  onClick={handleResetPassword}
                  disabled={resetLoading || !resetVerified}
                  className="flex-1 bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 disabled:opacity-50"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                >
                  {resetLoading ? "재설정 중..." : "비밀번호 재설정"}
                </button>
                <button
                  onClick={() => {
                    setShowResetPasswordModal(false);
                    setResetEmail("");
                    setResetPhone("");
                    setResetCode("");
                    setResetNewPassword("");
                    setResetCodeSent(false);
                    setResetVerified(false);
                    setResetError("");
                  }}
                  className="flex-1 bg-gray-500 text-white py-2 rounded-md hover:bg-gray-600"
                  style={{ fontFamily: "Noto Sans KR, 'Apple SD Gothic Neo', 'Nanum Gothic', system-ui, -apple-system, 'Segoe UI'" }}
                >
                  닫기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}