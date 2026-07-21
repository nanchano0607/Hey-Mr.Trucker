import { useEffect, useState, useMemo } from "react";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

function getConsentLabel(consent: UserConsent): string {
  return String(
    consent.consentType ??
      consent.type ??
      consent.name ??
      consent.title ??
      consent.code ??
      ""
  )
    .trim()
    .toLowerCase();
}

function getConsentAgreed(consent: UserConsent): boolean | undefined {
  const agreedRaw = consent.agreed ?? consent.consented ?? consent.isAgreed;
  return typeof agreedRaw === "boolean" ? agreedRaw : undefined;
}

function hasSmsConsent(user: User): boolean {
  const consents = user.consents ?? [];
  return consents.some((c) => {
    if (getConsentAgreed(c) !== true) return false;
    const label = getConsentLabel(c);
    return label.includes("sms") || label.includes("문자");
  });
}

function hasEmailConsent(user: User): boolean {
  const consents = user.consents ?? [];
  return consents.some((c) => {
    if (getConsentAgreed(c) !== true) return false;
    const label = getConsentLabel(c);
    return label.includes("email") || label.includes("e-mail") || label.includes("이메일") || label.includes("메일");
  });
}

type UserConsent = {
  id?: number;
  consentType?: string;
  type?: string;
  name?: string;
  title?: string;
  code?: string;
  version?: string;
  agreed?: boolean;
  consented?: boolean;
  isAgreed?: boolean;
  agreedAt?: string;
  consentedAt?: string;
  createdAt?: string;
  updatedAt?: string;
  [key: string]: any;
};

type User = {
  id: number;
  email: string | null;
  name: string;
  phone?: string | null;
  admin: boolean; // 백엔드 필드명 (isAdmin)
  createdAt: string;
  isDeleted: boolean; // 백엔드 필드명 (탈퇴/비활성 여부)
  oauthProvider?: string;
  consents?: UserConsent[];
};

interface UserManagementProps {
  isOpen: boolean;
  onToggle: () => void;
}

export default function UserManagement({ isOpen, onToggle }: UserManagementProps) {
  const [users, setUsers] = useState<User[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [selectAll, setSelectAll] = useState<boolean>(false);
  const [expandedUserIds, setExpandedUserIds] = useState<number[]>([]);
  const [couponToIssue, setCouponToIssue] = useState<string>("");
  const [issuing, setIssuing] = useState<boolean>(false);
  const [issueResults, setIssueResults] = useState<Array<{userId:number; ok:boolean; message:string}>>([]);
  const [loading, setLoading] = useState(false);
  const [loadingProgress, setLoadingProgress] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState("");
  const [userFilter, setUserFilter] = useState<string>("ALL");
  const [consentFilter, setConsentFilter] = useState<string>("ALL");

  const toggleConsents = (userId: number) => {
    setExpandedUserIds((prev) => (prev.includes(userId) ? prev.filter((id) => id !== userId) : [...prev, userId]));
  };

  useEffect(() => {
    // 컴포넌트 마운트 시 사용자 목록 가져오기 (개수 표시용)
    fetchUsers();
  }, []);

  useEffect(() => {
    if (isOpen) {
      fetchUsers();
    }
  }, [isOpen, userFilter]);

  const fetchUsers = async () => {
    setLoading(true);
    setLoadingProgress("사용자 ID 목록 조회 중...");
    try {
      const token = localStorage.getItem("access_token");
      
      // 1단계: 사용자 ID 목록만 먼저 가져오기
      const idsRes = await fetch(`${SERVER}/api/admin/users/ids`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      
      if (!idsRes.ok) {
        // 기존 API가 없는 경우 fallback으로 기존 방식 시도
        console.log("IDs API 없음, 기존 방식으로 fallback");
        setLoadingProgress("전체 사용자 목록 조회 중...");
        await fetchUsersLegacy();
        return;
      }
      
      const userIds = await idsRes.json();
      console.log("User IDs fetched:", userIds.length);
      setLoadingProgress(`${userIds.length}명의 사용자 정보 개별 조회 중...`);
      
      // 2단계: 각 사용자를 개별적으로 가져오기
      const users: User[] = [];
      const batchSize = 5; // 동시에 처리할 사용자 수
      
      for (let i = 0; i < userIds.length; i += batchSize) {
        const batch = userIds.slice(i, i + batchSize);
        const currentBatch = Math.floor(i/batchSize) + 1;
        const totalBatches = Math.ceil(userIds.length / batchSize);
        
        setLoadingProgress(`사용자 정보 조회 중... (${currentBatch}/${totalBatches} 배치, ${users.length}/${userIds.length}명 완료)`);
        
        const batchPromises = batch.map(async (userId: number) => {
          try {
            const userRes = await fetch(`${SERVER}/api/admin/users/${userId}`, {
              headers: token ? { Authorization: `Bearer ${token}` } : {},
            });
            
            if (userRes.ok) {
              const userData = await userRes.json();
              // 필요한 필드만 추출 (isDeleted 사용)
              return {
                id: userData.id,
                email: userData.email,
                name: userData.name,
                phone: userData.phone,
                admin: userData.admin || userData.isAdmin, // isAdmin도 체크
                createdAt: userData.createdAt,
                isDeleted: Boolean(userData.isDeleted ?? userData.deleted ?? false),
                oauthProvider: userData.oauthProvider,
                consents: Array.isArray(userData.consents) ? userData.consents : [],
              };
            } else {
              console.warn(`사용자 ${userId} 조회 실패:`, userRes.status);
              return null;
            }
          } catch (error) {
            console.warn(`사용자 ${userId} 조회 중 오류:`, error);
            return null;
          }
        });
        
        const batchResults = await Promise.all(batchPromises);
        const validUsers = batchResults.filter(user => user !== null) as User[];
        users.push(...validUsers);
        
        console.log(`Batch ${currentBatch} 완료: ${validUsers.length}/${batch.length} 사용자`);
      }
      
      console.log("총 사용자 로드 완료:", users.length);
      setLoadingProgress("");
      setUsers(users);
      
    } catch (e) {
      console.error("사용자 목록 조회 실패:", e);
      setLoadingProgress("오류 발생, 기존 방식으로 재시도 중...");
      // fallback으로 기존 방식 시도
      await fetchUsersLegacy();
    } finally {
      setLoading(false);
      setLoadingProgress("");
    }
  };

  // 기존 방식 (fallback용)
  const fetchUsersLegacy = async () => {
    try {
      const token = localStorage.getItem("access_token");
      const res = await fetch(`${SERVER}/api/admin/users`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      
      if (res.ok) {
        const text = await res.text();
        console.log("Legacy API Response status:", res.status);
        
        // 응답이 비어있는 경우 처리
        if (!text || text.trim() === '') {
          console.warn("Empty response received");
          setUsers([]);
          return;
        }
        
        // JSON 파싱 시도
        try {
          const data = JSON.parse(text);
          console.log("Successfully parsed JSON, user count:", data.length);
          
          // 필요한 필드만 추출하여 순환 참조 문제 해결 (isDeleted 사용)
          const cleanUsers = data.map((user: any) => ({
            id: user.id,
            email: user.email,
            name: user.name,
            phone: user.phone,
            admin: user.admin || user.isAdmin,
            createdAt: user.createdAt,
            isDeleted: Boolean(user.isDeleted ?? user.deleted ?? false),
            oauthProvider: user.oauthProvider,
            consents: Array.isArray(user.consents) ? user.consents : [],
          }));
          
          setUsers(cleanUsers);
          
        } catch (parseError) {
          console.error("JSON 파싱 실패:", parseError);
          console.error("응답 텍스트 길이:", text.length);
          
          // 백엔드 중첩 깊이 오류인 경우
          if (text.includes('nesting depth') || text.includes('maximum allowed')) {
            alert("서버에서 데이터 처리 중 오류가 발생했습니다. 개별 사용자 API 구현이 필요합니다.");
          }
          
          // 목업 데이터로 대체

        }
      } else {
        console.error("API 응답 실패:", res.status, res.statusText);
        alert(`사용자 목록 조회 실패: ${res.status} ${res.statusText}`);
      }
    } catch (e) {
      console.error("Legacy 사용자 목록 조회 실패:", e);
      alert("사용자 목록 조회 중 네트워크 오류가 발생했습니다.");
    }
  };

  const toggleUserStatus = async (userId: number, currentIsDeleted: boolean) => {
    const action = currentIsDeleted ? "비활성화" : "활성화";
    if (!confirm(`이 사용자를 ${action}하시겠습니까?`)) return;

    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/admin/users/${userId}/toggle-status`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });

      if (response.ok) {
        const result = await response.json();
        alert(result.message);
        
        // 로컬 상태 업데이트
        setUsers(prevUsers =>
          prevUsers.map(user =>
            user.id === userId
              ? { ...user, isDeleted: Boolean(result.deleted) }
              : user
          )
        );
      } else if (response.status === 404) {
        alert("사용자를 찾을 수 없습니다.");
      } else {
        alert("상태 변경에 실패했습니다.");
      }
    } catch (error) {
      console.error("상태 변경 실패:", error);
      alert("상태 변경 중 오류가 발생했습니다.");
    }
  };

  const toggleAdminRole = async (userId: number) => {
    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/admin/users/${userId}/toggle-admin`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });

      if (response.ok) {
        const result = await response.json();
        alert(result.message);
        
        // 로컬 상태 업데이트
        setUsers(prevUsers => 
          prevUsers.map(user => 
            user.id === userId 
              ? { ...user, admin: result.admin }
              : user
          )
        );
      } else if (response.status === 404) {
        alert("사용자를 찾을 수 없습니다.");
      } else {
        alert("권한 변경에 실패했습니다.");
      }
    } catch (error) {
      console.error("권한 변경 실패:", error);
      alert("권한 변경 중 오류가 발생했습니다.");
    }
  };

  // 클라이언트 사이드 필터링 (memoized)
  const filteredUsers = useMemo(() => {
    return users.filter(user => {
      const q = searchTerm.toLowerCase();
      const matchesSearch = user.email?.toLowerCase().includes(q) || user.name?.toLowerCase().includes(q);
      const matchesFilter =
        userFilter === "ALL" ||
        (userFilter === "ACTIVE" && !user.isDeleted) ||
        (userFilter === "INACTIVE" && user.isDeleted) ||
        (userFilter === "ADMIN" && user.admin) ||
        (userFilter === "USER" && !user.admin);
      const matchesConsent =
        consentFilter === "ALL" ||
        (consentFilter === "SMS" && hasSmsConsent(user)) ||
        (consentFilter === "EMAIL" && hasEmailConsent(user));
      return matchesSearch && matchesFilter && matchesConsent;
    });
  }, [users, searchTerm, userFilter, consentFilter]);

  // selectAll 반응: 필터된 사용자에 대해 전체선택 상태를 동기화
  useEffect(() => {
    if (selectAll) {
      const ids = filteredUsers.map(u => u.id);
      // 중복 setState 방지: 동일한 selection이면 업데이트 안함
      const same = ids.length === selectedIds.length && ids.every((v, i) => v === selectedIds[i]);
      if (!same) setSelectedIds(ids);
    } else {
      const idSet = new Set(filteredUsers.map(u => u.id));
      const next = selectedIds.filter(id => idSet.has(id));
      const same = next.length === selectedIds.length && next.every((v, i) => v === selectedIds[i]);
      if (!same) setSelectedIds(next);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectAll, filteredUsers]);

  return (
    <div className="border p-4 rounded font-sans">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-xl font-semibold">사용자 관리</h2>
          <span className="text-sm text-orange-800">({users.length})</span>
        </div>
        <button
          onClick={onToggle}
          className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
        >
          {isOpen ? "접기" : "펼치기"}
        </button>
      </div>
      
      {isOpen && (
        <div className="mt-4">
          {/* 검색 및 필터 */}
          <div className="mb-4 space-y-3">
            <div className="flex items-center gap-4 flex-wrap">
              <div className="flex items-center gap-2">
              <input
                id="select-all-users"
                type="checkbox"
                checked={selectAll}
                onChange={(e) => {
                  const checked = e.target.checked;
                  setSelectAll(checked);
                  if (checked) {
                    setSelectedIds(filteredUsers.map(u => u.id));
                  } else {
                    // 즉시 전체 해제
                    setSelectedIds([]);
                  }
                }}
                className="w-4 h-4"
              />
              <label htmlFor="select-all-users" className="text-sm">전체선택</label>
              </div>

            <select
              className="border rounded px-2 py-1 text-sm"
              value={userFilter}
              onChange={(e) => setUserFilter(e.target.value)}
            >
              <option value="ALL">전체</option>
              <option value="ACTIVE">활성 회원</option>
              <option value="INACTIVE">비활성 회원</option>
              <option value="ADMIN">관리자</option>
              <option value="USER">일반 회원</option>
            </select>

            <select
              className="border rounded px-2 py-1 text-sm"
              value={consentFilter}
              onChange={(e) => setConsentFilter(e.target.value)}
            >
              <option value="ALL">동의 전체</option>
              <option value="SMS">SMS 동의자</option>
              <option value="EMAIL">Email 동의자</option>
            </select>
            <button
              onClick={fetchUsers}
              className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
            >
              새로고침
            </button>
            </div>

            <div>
              <input
                type="text"
                placeholder="이름 또는 이메일로 검색..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full border rounded px-3 py-2 text-base"
              />
            </div>
          </div>

          {/* 사용자 목록 */}
          {loading ? (
            <div className="text-center py-8">
              <div className="text-black mb-2">로딩 중...</div>
              {loadingProgress && (
                <div className="text-sm text-black">{loadingProgress}</div>
              )}
            </div>
          ) : filteredUsers.length === 0 ? (
            <p className="text-black">사용자가 없습니다.</p>
          ) : (
            <div className="space-y-3">
              {filteredUsers.map((user) => (
                <div key={user.id} className="border rounded p-4">
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(user.id)}
                          onChange={(e) => {
                            const checked = e.target.checked;
                            setSelectedIds(prev => {
                              const next = checked ? Array.from(new Set([...prev, user.id])) : prev.filter(id => id !== user.id);
                              // 전체 선택 상태 업데이트
                              setSelectAll(next.length > 0 && next.length === filteredUsers.length);
                              return next;
                            });
                          }}
                          className="w-4 h-4 mt-1 mr-2"
                        />
                        <h3 className="font-semibold">{user.name}</h3>
                        {user.admin && (
                          <span className="px-2 py-1 bg-purple-100 text-purple-800 text-xs rounded-4">
                            관리자
                          </span>
                        )}
                        {!user.isDeleted && (
                          <span className="px-2 py-1 bg-red-100 text-red-800 text-xs rounded-full">
                            비활성
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-black">이메일: {user.email}</p>
                      {user.phone && (
                        <p className="text-sm text-black">전화번호: {user.phone}</p>
                      )}
                      <p className="text-xs text-black">
                        가입일: {new Date(user.createdAt).toLocaleDateString("ko-KR")}
                      </p>
                    </div>
                  </div>

                  {expandedUserIds.includes(user.id) && (
                    <div className="mt-3 p-3 border rounded bg-white/5">
                      <div className="text-sm font-semibold mb-2">동의 항목</div>
                      {(user.consents ?? []).length === 0 ? (
                        <div className="text-sm text-black/70">동의 내역이 없습니다.</div>
                      ) : (
                        <div className="space-y-2">
                          {(user.consents ?? []).map((c, idx) => {
                            const name = c.consentType ?? c.type ?? c.name ?? c.title ?? c.code ?? `항목 ${idx + 1}`;
                            const agreedRaw = c.agreed ?? c.consented ?? c.isAgreed;
                            const agreed = typeof agreedRaw === "boolean" ? agreedRaw : undefined;
                            const at = c.agreedAt ?? c.consentedAt ?? c.updatedAt ?? c.createdAt;

                            return (
                              <div key={c.id ?? `${user.id}-${idx}`} className="text-sm text-black">
                                <div className="font-medium">
                                  {name}{" "}
                                  {agreed === undefined ? "" : agreed ? "(동의)" : "(미동의)"}
                                </div>
                                {at && <div className="text-xs text-black/70">일시: {new Date(at).toLocaleString("ko-KR")}</div>}
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  )}

                  <div className="flex gap-2 justify-end pt-3 border-t">
                    <button
                      onClick={() => toggleConsents(user.id)}
                      className="px-3 py-1.5 text-white text-sm rounded bg-blue-600 hover:bg-blue-700"
                    >
                      {expandedUserIds.includes(user.id) ? "동의 닫기" : "동의 보기"}
                    </button>
                    <button
                      onClick={() => toggleUserStatus(user.id, user.isDeleted)}
                      className={`px-3 py-1.5 text-white text-sm rounded ${
                        user.isDeleted 
                          ? "bg-red-600 hover:bg-red-700"  // 비활성(삭제됨) → 활성화로 변경
                          : "bg-green-600 hover:bg-green-700"      // 활성 → 비활성화로 변경
                      }`}
                      title={user.isDeleted ? "활성화로 변경" : "비활성화로 변경"}
                    >
                      {user.isDeleted ? "비활성화로 변경" : "활성화로 변경"}
                    </button>
                    
                    <button
                      onClick={() => toggleAdminRole(user.id)}
                      className={`px-3 py-1.5 text-white text-sm rounded ${
                        user.admin 
                          ? "bg-gray-600 hover:bg-gray-700" 
                          : "bg-purple-600 hover:bg-purple-700"
                      }`}
                    >
                      {user.admin ? "관리자 해제" : "관리자 승격"}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
          
          {/* 쿠폰 발급 영역 */}
          <div className="mt-4 p-3 border rounded bg-white/5">
            <div className="flex items-center gap-2 mb-2">
              <input type="text" placeholder="발급할 쿠폰 ID 입력" value={couponToIssue} onChange={(e) => setCouponToIssue(e.target.value)} className="p-2 border rounded w-48" />
              <button
                onClick={async () => {
                  if (!couponToIssue) return alert('쿠폰 ID를 입력하세요');
                  if (selectedIds.length === 0) return alert('발급할 사용자를 하나 이상 선택하세요');
                  if (!confirm(`${selectedIds.length}명에게 쿠폰(${couponToIssue})을 발급하시겠습니까?`)) return;
                  setIssuing(true);
                  setIssueResults([]);
                  const results: Array<{userId:number; ok:boolean; message:string}> = [];
                  for (const userId of selectedIds) {
                    try {
                      const token = localStorage.getItem('access_token');
                      const res = await fetch(`${SERVER}/api/user-coupons/admin/issue/${couponToIssue}?userId=${userId}`, {
                        method: 'POST',
                        headers: {
                          'Content-Type': 'application/json',
                          ...(token ? { Authorization: `Bearer ${token}` } : {}),
                        },
                        credentials: 'include'
                      });
                      if (res.ok) {
                        results.push({ userId, ok: true, message: '성공' });
                      } else {
                        const b = await res.json().catch(()=>({}));
                        results.push({ userId, ok: false, message: b?.error || `HTTP ${res.status}` });
                      }
                    } catch (err:any) {
                      results.push({ userId, ok: false, message: err?.message || '네트워크 오류' });
                    }
                  }
                  setIssueResults(results);
                  setIssuing(false);
                  // 선택 해제
                  setSelectedIds([]);
                  setSelectAll(false);
                }}
                className="px-3 py-1 bg-blue-600 text-white rounded text-sm"
                disabled={issuing}
              >
                {issuing ? '발급중...' : `선택 ${selectedIds.length}명에게 발급`}
              </button>
            </div>

            {issueResults.length > 0 && (
              <div className="mt-2 text-sm">
                <div className="font-medium">발급 결과</div>
                <ul className="mt-1">
                  {issueResults.map(r => (
                    <li key={r.userId} className={r.ok ? 'text-green-600' : 'text-red-600'}>User #{r.userId}: {r.message}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}