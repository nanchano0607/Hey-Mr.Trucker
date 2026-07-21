import { useState, useMemo, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../../config/apiBase";

const API = API_BASE_URL;
const SERVER = API_BASE_URL;

export default function SignupPhone() {
  const navigate = useNavigate();
  const params = useMemo(() => new URLSearchParams(window.location.search), []);
  const token = params.get("token") ?? "";

  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [code, setCode] = useState("");
  const [codeSent, setCodeSent] = useState(false);
  const [codeLoading, setCodeLoading] = useState(false);
  const [verifyLoading, setVerifyLoading] = useState(false);
  const [verified, setVerified] = useState(false);
  const [sendMessage, setSendMessage] = useState<string | null>(null);
  const [verifyMessage, setVerifyMessage] = useState<string | null>(null);
  const [timerSeconds, setTimerSeconds] = useState(0);
  const timerRef = useRef<number | null>(null);
  const [resendCooldown, setResendCooldown] = useState(0);
  const resendRef = useRef<number | null>(null);

  // 약관 동의 상태
  const [agreements, setAgreements] = useState({
    TERMS: false,
    PRIVACY: false,
    OVER14: false,
    MARKETING_EMAIL: false,
    MARKETING_SMS: false,
  });

  // 약관 내용
  const agreementContents = {
    TERMS: {
      title: "이용약관 동의",
      required: true,
      content: `본 약관은 HEY! MR. TRUCKER(이하 "회사")가 제공하는 회원 서비스 및 온라인 쇼핑몰 이용과 관련하여 회사와 회원 간의 권리, 의무 및 책임사항을 규정합니다.

회원은 본 약관에 동의함으로써 회사가 제공하는 상품 구매, 콘텐츠 이용, 고객지원 등 모든 서비스를 이용할 수 있습니다.

회사는 관련 법령을 준수하며, 약관 변경 시 사전 공지를 통해 회원에게 안내합니다.`
    },
    PRIVACY: {
      title: "개인정보 수집·이용 동의",
      required: true,
      content: `회사는 회원가입 및 서비스 제공을 위해 아래와 같은 개인정보를 수집·이용합니다.

• 수집 항목: 이메일, 비밀번호(암호화), 이름, 전화번호
• 수집 목적: 회원 식별, 본인 확인, 주문·배송·결제 처리, 고객지원
• 보유 기간: 회원 탈퇴 시까지 (관계 법령에 따라 보존 필요 시 해당 기간)

회원은 개인정보 수집·이용에 대한 동의를 거부할 수 있으나, 필수 항목 미동의 시 회원가입 및 서비스 이용이 제한됩니다.`
    },
    OVER14: {
      title: "만 14세 이상 확인",
      required: true,
      content: `본인은 만 14세 이상이며, HEY! MR. TRUCKER의 회원으로 가입하여 서비스를 이용할 수 있는 법적 요건을 충족함을 확인합니다.

만 14세 미만인 경우 회원가입이 제한될 수 있습니다.`
    },
    MARKETING_EMAIL: {
      title: "마케팅 이메일 수신 동의",
      required: false,
      content: `회사는 할인, 이벤트, 신상품 안내 등 유용한 정보를 이메일로 제공할 수 있습니다.

본 동의는 선택 사항이며, 동의하지 않아도 서비스 이용에는 어떠한 제한도 없습니다.

수신 동의는 마이페이지에서 언제든지 변경 또는 철회할 수 있습니다.`
    },
    MARKETING_SMS: {
      title: "마케팅 SMS 수신 동의",
      required: false,
      content: `회사는 이벤트, 프로모션, 중요 혜택 정보를 문자메시지(SMS)로 발송할 수 있습니다.

본 동의는 선택 사항이며, 동의 여부와 관계없이 회원가입 및 서비스 이용이 가능합니다.

수신 동의는 마이페이지에서 언제든지 변경 또는 철회할 수 있습니다.`
    }
  } as const;

  // 전체 동의 처리
  const handleAllAgree = (checked: boolean) => {
    setAgreements({
      TERMS: checked,
      PRIVACY: checked,
      OVER14: checked,
      MARKETING_EMAIL: checked,
      MARKETING_SMS: checked,
    });
  };

  // 개별 동의 처리
  const handleAgreementChange = (key: keyof typeof agreements) => {
    setAgreements((prev) => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  // 필수 동의 확인
  const isRequiredAgreed =
    agreements.TERMS && agreements.PRIVACY && agreements.OVER14;

  useEffect(() => {
    // on mount, try to prefill phone from signup draft if present
    try {
      const draftRaw = sessionStorage.getItem("signupDraft");
      console.debug("signupDraft (raw) on mount:", draftRaw);
      if (draftRaw) {
        const draft = JSON.parse(draftRaw);
        console.debug("signupDraft (parsed) on mount:", draft);
        if (draft?.phone) setPhone(draft.phone);
        // Load agreements from draft if present (for standard signup)
        if (draft?.agreements) {
          setAgreements(draft.agreements);
        }
      }
    } catch (e) {
      // ignore
    }

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
      if (resendRef.current) {
        clearInterval(resendRef.current);
        resendRef.current = null;
      }
    };
  }, []);

  const startResendCooldown = (seconds = 30) => {
    if (resendRef.current) {
      clearInterval(resendRef.current);
      resendRef.current = null;
    }
    setResendCooldown(seconds);
    resendRef.current = window.setInterval(() => {
      setResendCooldown((prev) => {
        if (prev <= 1) {
          if (resendRef.current) {
            clearInterval(resendRef.current);
            resendRef.current = null;
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000) as unknown as number;
  };

  const startTimer = (seconds = 300) => {
    // clear existing
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    setTimerSeconds(seconds);
    timerRef.current = window.setInterval(() => {
      setTimerSeconds((prev) => {
        if (prev <= 1) {
          if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000) as unknown as number;
  };

  const formatTime = (s: number) => {
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${m}:${sec.toString().padStart(2, "0")}`;
  };

  const validatePhone = (p: string) => {
    // 간단 검증: 숫자만 9~12자리 허용 (프로젝트 규칙에 맞게 조정 가능)
    return /^\d{9,12}$/.test(p);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!validatePhone(phone)) return setError("올바른 전화번호(숫자만, 9~12자리)를 입력하세요.");
    if (!verified) return setError("전화번호 인증을 먼저 완료하세요.");
    if (!isRequiredAgreed) return setError("필수 동의 항목을 모두 체크해주세요.");

    setLoading(true);
    try {
      if (token) {
        // social signup completion
        const payload = { token, phone, agreements };
        console.debug("POST -> /api/auth/complete-signup", payload);
        const res = await fetch(`${API}/api/auth/complete-signup`, {
          method: "POST",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });
        
        const text = await res.text().catch(() => "");
        let parsed: any = text;
        try {
          parsed = text ? JSON.parse(text) : {};
        } catch (e) {
          // ignore, keep parsed as empty object
        }
        
        if (!res.ok) {
          const message = parsed?.error || parsed?.message || (typeof parsed === "string" && parsed.trim().length > 0 ? parsed : `HTTP ${res.status}`);
          setError(message);
          console.error("Social signup failed:", message, parsed);
          return;
        }
        
        // success -> navigate to login
        console.debug("Social signup success:", parsed);
        sessionStorage.removeItem("signupDraft");
        navigate("/login");
      } else {
        // standard signup: read draft and send signup request with phone
        const draftRaw = sessionStorage.getItem("signupDraft");
        console.debug("signupDraft (raw) before signup:", draftRaw);
        if (!draftRaw) throw new Error("회원가입 정보가 없습니다. 다시 시도하세요.");
        const draft = JSON.parse(draftRaw);
        console.debug("signupDraft (parsed) before signup:", draft);
        if (!draft.email) console.warn("signupDraft.email is missing or null", draft);
        const payload = {
          email: draft.email,
          password: draft.password,
          name: draft.name,
          agreements: agreements, // Use the agreements from SignupPhone form
          phone,
        };
        console.debug("POST -> /auth/signup", payload);
        const res = await fetch(`${API}/api/auth/signup`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });
        // read as text because backend may return plain string on error
        const text = await res.text().catch(() => "");
        let parsed: any = text;
        try {
          parsed = text ? JSON.parse(text) : text;
        } catch (e) {
          // keep parsed as raw text
        }
        if (!res.ok) {
          const message = typeof parsed === "string" && parsed.trim().length > 0 ? parsed : (parsed?.message || parsed?.error || `HTTP ${res.status}`);
          setError(message);
          return;
        }
        // success
        sessionStorage.removeItem("signupDraft");
        alert("회원가입이 완료되었습니다! 로그인해 주세요.");
        navigate(`/login`, { replace: true });
      }
    } catch (e: any) {
      console.error("회원가입 완료 중 오류:", e);
      setError(e?.message || "처리 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleSendCode = async () => {
    setSendMessage(null);
    setVerifyMessage(null);
    setError(null);
    // clear previous code/verified state when resending
    setCode("");
    setVerified(false);
    if (!validatePhone(phone)) return setError("올바른 전화번호(숫자만, 9~12자리)를 입력하세요.");
    setCodeLoading(true);
    try {
      const res = await fetch(`${API}/api/phone/send`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ phoneNumber: phone }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) {
        // handle rate-limit explicitly
        if (res.status === 429) {
          const ra = res.headers.get("Retry-After");
          const secs = ra ? parseInt(ra, 10) || 60 : 60;
          startResendCooldown(secs);
          setError(body?.message || body?.error || "너무 잦은 요청입니다. 잠시 후 다시 시도하세요.");
          return;
        }
        throw new Error(body?.message || body?.error || `HTTP ${res.status}`);
      }
      setCodeSent(true);
      startTimer(300); // 5분 = 300초
      setSendMessage("인증번호가 전송되었습니다. 도착하지 않으면 번호를 확인하세요.");
    } catch (e: any) {
      console.error("코드 전송 오류:", e);
      setError(e?.message || "코드 전송 중 오류가 발생했습니다.");
      // as fallback, set a short client-side cooldown to avoid hammering
      if (!resendRef.current) startResendCooldown(30);
    } finally {
      setCodeLoading(false);
    }
  };

  const handleVerifyCode = async () => {
    setVerifyMessage(null);
    setError(null);
    if (!validatePhone(phone)) return setError("전화번호를 먼저 입력하세요.");
    if (!code || code.trim().length === 0) return setError("인증번호를 입력하세요.");
    setVerifyLoading(true);
    try {
      const res = await fetch(`${API}/api/phone/verify`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ phoneNumber: phone, code: code.trim() }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body?.message || body?.error || `HTTP ${res.status}`);
      setVerified(true);
      setVerifyMessage("전화번호 인증이 완료되었습니다.");
    } catch (e: any) {
      console.error("코드 검증 오류:", e);
      setError(e?.message || "인증번호가 올바르지 않습니다.");
    } finally {
      setVerifyLoading(false);
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
          className="relative bg-[#000] mt-12"
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
            {/* 가장 안쪽 컨텐츠 (#F5DEB3) */}
            <div 
              className="w-full px-8 py-6 bg-[#F5DEB3]"
              style={{
                minWidth: '400px',
                imageRendering: 'pixelated',
                clipPath: `polygon(
                  0% 16px, 16px 16px, 16px 0%,
                  calc(100% - 16px) 0%, calc(100% - 16px) 16px, 100% 16px,
                  100% calc(100% - 16px), calc(100% - 16px) calc(100% - 16px), calc(100% - 16px) 100%,
                  16px 100%, 16px calc(100% - 16px), 0% calc(100% - 16px)
                )`
              }}
            >
          <h1 className="text-2xl font-semibold mb-8" style={{ imageRendering: 'pixelated' }}>전화번호 확인</h1>

          {/* 기존 SignupPhone 폼을 LoginPage 스타일 내부로 옮김 */}
          <form onSubmit={handleSubmit} className="space-y-4 mb-6">
            <div className="w-64">
              <label className="block text-sm font-medium mb-1">전화번호</label>
              <input
                value={phone}
                onChange={(e) => {
                  const newPhone = e.target.value.replace(/[^0-9]/g, "");
                  setPhone(newPhone);
                  // If the phone changes after sending or verifying a code,
                  // invalidate previous send/verification so user must re-verify.
                  if (codeSent || verified) {
                    setCodeSent(false);
                    setVerified(false);
                    setCode("");
                    setSendMessage(null);
                    setVerifyMessage(null);
                    setTimerSeconds(0);
                    if (timerRef.current) {
                      clearInterval(timerRef.current);
                      timerRef.current = null;
                    }
                    if (resendRef.current) {
                      clearInterval(resendRef.current);
                      resendRef.current = null;
                    }
                    setResendCooldown(0);
                  }
                }}
                placeholder="숫자만 입력 (예: 01012341234)"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                style={{ fontFamily: 'sans-serif' }}
                inputMode="numeric"
                required
              />
            </div>

            <div className="flex gap-2 items-center">
              {(() => {
                const blocked = codeLoading || verified || resendCooldown > 0; // block while client-side cooldown active
                  return (
                    <button
                      type="button"
                      onClick={() => { if (!blocked) handleSendCode(); }}
                      disabled={blocked}
                      className={`py-2 px-3 rounded text-white ${blocked ? 'bg-gray-400 cursor-not-allowed opacity-60' : 'bg-gray-800'}`}
                    >
                      {codeLoading ? "전송 중..." : codeSent ? "재전송" : "인증번호 전송"}
                    </button>
                  );
              })()}

              {codeSent && (
                <div className="flex-1 flex gap-2 flex-col">
                  <div className="text-sm text-gray-600">{timerSeconds > 0 ? `유효시간: ${formatTime(timerSeconds)}` : '인증번호가 만료되었습니다. 재전송하세요.'}</div>
                  <div className="flex gap-2">
                    <input
                      value={code}
                      onChange={(e) => setCode(e.target.value.replace(/[^0-9]/g, ""))}
                      placeholder="인증번호 입력"
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      style={{ fontFamily: 'sans-serif' }}
                      inputMode="numeric"
                    />
                    <button
                      type="button"
                      onClick={handleVerifyCode}
                      className={`py-2 px-3 rounded ${verified ? 'bg-green-600 text-white' : 'bg-blue-600 text-white'}`}
                      disabled={verifyLoading || verified || timerSeconds === 0}
                    >
                      {verifyLoading ? '확인 중...' : verified ? '인증됨' : '인증 확인'}
                    </button>
                  </div>
                </div>
              )}
          </div>

          {sendMessage && <div className="text-sm text-green-600">{sendMessage}</div>}
          {verifyMessage && <div className="text-sm text-green-600">{verifyMessage}</div>}
          {resendCooldown > 0 && (
            <div className="text-sm text-orange-600">재전송 가능까지: {formatTime(resendCooldown)}</div>
          )}
          {error && <div className="text-sm text-red-600">{error}</div>}

            {/* 약관동의 섹션 */}
            <div className="border-t pt-4 mt-6 max-h-96 overflow-y-auto">
              <h3 className="text-sm font-medium mb-3">약관 동의</h3>

              {/* 전체 동의 */}
              <div className="mb-3 pb-3 border-b">
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={Object.values(agreements).every(Boolean)}
                    onChange={(e) => handleAllAgree(e.target.checked)}
                    className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                  <span className="text-sm font-medium">전체 동의</span>
                </label>
              </div>

              {/* 필수 동의 */}
              <div className="space-y-2 mb-3">
                <p className="text-xs text-gray-600 font-medium">[필수 동의]</p>
                {(['TERMS', 'PRIVACY', 'OVER14'] as const).map((key) => (
                  <div key={key} className="border rounded bg-white">
                    <div className="flex items-center space-x-2 p-2 border-b">
                      <input
                        type="checkbox"
                        checked={agreements[key]}
                        onChange={() => handleAgreementChange(key)}
                        className="rounded border-gray-300 text-blue-600 focus:ring-blue-500 cursor-pointer"
                      />
                      <span className="text-sm font-medium flex-1">{agreementContents[key].title}</span>
                      <span className="text-red-500 text-xs">*</span>
                    </div>
                    <div className="px-2 py-2 bg-gray-50 text-xs text-gray-700 whitespace-pre-wrap max-h-32 overflow-y-auto">
                      {agreementContents[key].content}
                    </div>
                  </div>
                ))}
              </div>

              {/* 선택 동의 */}
              <div className="space-y-2">
                <p className="text-xs text-gray-600 font-medium">[선택 동의]</p>
                {(['MARKETING_EMAIL', 'MARKETING_SMS'] as const).map((key) => (
                  <div key={key} className="border rounded bg-white">
                    <div className="flex items-center space-x-2 p-2 border-b">
                      <input
                        type="checkbox"
                        checked={agreements[key]}
                        onChange={() => handleAgreementChange(key)}
                        className="rounded border-gray-300 text-blue-600 focus:ring-blue-500 cursor-pointer"
                      />
                      <span className="text-sm font-medium flex-1">{agreementContents[key].title}</span>
                    </div>
                    <div className="px-2 py-2 bg-gray-50 text-xs text-gray-700 whitespace-pre-wrap max-h-32 overflow-y-auto">
                      {agreementContents[key].content}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="flex gap-2">
              <button
                type="submit"
                className="w-full h-11 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
                disabled={loading || !verified || !isRequiredAgreed}
              >
                {loading ? "전송 중..." : "가입 완료"}
              </button>
            </div>
          </form>

          
            </div>
          </div>
        </div>
      </div>
      
      {/* 레이아웃 높이 확보 */}
      <div style={{ height: "100vh" }} />
    </section>
  );
}
