import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../../config/apiBase";

const API = API_BASE_URL;

export default function SignUp() {
  const { search } = useLocation();
  const params = new URLSearchParams(search);
  const redirect = params.get("redirect") || "/";
  const navigate = useNavigate();

  // 회원가입 폼 상태
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [name, setName] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [idChecked, setIdChecked] = useState(false);
  const [idAvailable, setIdAvailable] = useState(false);
  const [idCheckLoading, setIdCheckLoading] = useState(false);

  const validateEmail = (e: string) => {
    if (!e) return false;
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e.trim());
  };

  // 회원가입 처리
  const handleSignUp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    // 비밀번호 확인 검증
    if (password !== confirmPassword) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }

    // 비밀번호 길이 검증
    if (password.length < 6) {
      setError("비밀번호는 최소 6자 이상이어야 합니다.");
      return;
    }

    setIsLoading(true);
    try {
        // store current signup data in sessionStorage and navigate to phone verification
      // backend expects an `email` field, so save the entered email under `email`
        const draft = {
            email: email,
            password,
            name,
          };
      sessionStorage.setItem("signupDraft", JSON.stringify(draft));
      // navigate to phone verification step (SignupPhone)
      navigate(`/complete-signup`);
    } catch (err: any) {
      setError("다음 단계로 이동 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-400">
      <div className="max-w-md mx-auto px-4 sm:px-6 lg:px-8 py-24">
        <h1 className="text-2xl font-semibold mb-2">회원가입</h1>

      <form onSubmit={handleSignUp} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">이름</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            style={{ fontFamily: 'sans-serif' }}
            placeholder="이름을 입력하세요"
            required
          />
        </div>


        <div>
          <label className="block text-sm font-medium mb-1">이메일</label>
          <div className="flex gap-2">
            <input
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setIdChecked(false);
                setIdAvailable(false);
              }}
              className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              style={{ fontFamily: 'sans-serif' }}
              placeholder="example@gmail.com"
              required
            />
            <button
              type="button"
              onClick={async () => {
                setError("");
                const raw = email || "";
                const candidate = raw.trim().toLowerCase();
                if (!candidate) {
                  setError("이메일을 입력하세요.");
                  return;
                }
                if (!validateEmail(candidate)) {
                  setError("유효한 이메일 주소를 입력하세요.");
                  return;
                }
                setIdCheckLoading(true);
                try {
                  const res = await fetch(`${API}/api/user/id/overlap`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email: candidate }),
                  });
                  // API may return boolean or an object; handle both
                  const body = await res.json().catch(() => null);
                  if (!res.ok) {
                    // try to extract message
                    const msg = (body && (body.message || body.error)) || `HTTP ${res.status}`;
                    throw new Error(msg);
                  }
                  let exists = false;
                  if (typeof body === "boolean") exists = body;
                  else if (body && typeof body === "object") {
                    if (typeof body.exists === "boolean") exists = body.exists;
                    else if (typeof body.value === "boolean") exists = body.value;
                    else {
                      // fallback: if response has a truthy length or count, treat as exists
                      exists = !!(body.count || body.length);
                    }
                  }
                  setIdAvailable(!exists);
                  setIdChecked(true);
                  if (exists) setError("이미 사용 중인 이메일입니다.");
                } catch (err: any) {
                  setError(err?.message || "이메일 확인 중 오류가 발생했습니다.");
                } finally {
                  setIdCheckLoading(false);
                }
              }}
              className="px-3 py-2 bg-gray-800 text-white rounded disabled:opacity-50"
              disabled={idCheckLoading}
            >
              {idCheckLoading ? "확인중..." : "중복확인"}
            </button>
          </div>
          <div className={`text-sm mt-1 ${idAvailable ? 'text-green-600' : 'text-red-600'}`}>
            {!validateEmail(email) ? (
              '유효한 이메일 주소를 입력하세요.'
            ) : idChecked ? (
              idAvailable ? '사용 가능한 이메일입니다.' : '이미 사용 중인 이메일입니다.'
            ) : (
              ''
            )}
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            style={{ fontFamily: 'sans-serif' }}
            placeholder="최소 6자 이상"
            required
            minLength={6}
          />
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">비밀번호 확인</label>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            style={{ fontFamily: 'sans-serif' }}
            placeholder="비밀번호를 다시 입력하세요"
            required
          />
        </div>

        {error && !/이메일|유효한 이메일|이미 사용 중인 이메일|이메일을 입력하세요/i.test(error) && (
          <div className="text-red-500 text-sm">{error}</div>
        )}

        <button
          type="submit"
          disabled={isLoading || !idChecked || !idAvailable}
          className="w-full h-11 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
        >
          {isLoading ? "넘어가는 중..." : "다음"}
        </button>
      </form>

      {/* 로그인 링크 */}
      <div className="mt-6 text-center">
        <p className="text-sm text-gray-600">
          이미 계정이 있으신가요?{" "}
          <Link
            to={`/login?redirect=${encodeURIComponent(redirect)}`}
            className="text-blue-600 hover:text-blue-500 font-medium"
          >
            로그인
          </Link>
        </p>
      </div>
      </div>
    </div>
  );
}