import { Outlet, Link, NavLink, useLocation } from "react-router-dom";
import { useState, useEffect, useMemo } from "react";
import { isMobileDevice } from "../utils/isMobile";
import { useAuth } from "../auth/useAuth";
import Footer from "../components/Footer";
import SitePopup from "../components/SitePopup";
import { API_BASE_URL } from "../config/apiBase";

const SERVER_URL = API_BASE_URL;

type MenuItem = {
  to: string;
  label: string;
  children?: MenuItem[];
};

const INSTAGRAM_URL =
  ((import.meta as any).env?.VITE_INSTAGRAM_URL as string | undefined) ||
  "https://www.instagram.com/hey_mr.trucker?igsh=MWw0bmg3Z3c2ZndhbA==";
const KAKAO_URL = ((import.meta as any).env?.VITE_KAKAO_URL as string | undefined) || "http://pf.kakao.com/_eGyxon";

// 안전한 src 변환: 빈 문자열/공백/undefined/null -> undefined 로 변환
const safeSrc = (src?: string | null) =>
  src && src.trim() !== "" ? src : undefined;

export default function RootLayout() {
  const { pathname } = useLocation();
  const [logoUrl, setLogoUrl] = useState<string | null>(null);
  const { user } = useAuth();

  const [isMobile, setIsMobile] = useState<boolean>(isMobileDevice());

  useEffect(() => {
    const onResize = () => setIsMobile(isMobileDevice());
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  // ✅ 모바일 메뉴 열림 상태
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  // 모바일: 하위메뉴 확장 상태 저장 (key: item.to)
  const [expandedItems, setExpandedItems] = useState<Record<string, boolean>>({});

  const toggleExpanded = (key: string) =>
    setExpandedItems((prev) => ({ ...prev, [key]: !prev[key] }));

  const hasActivePath = (item: MenuItem): boolean => {
    const selfActive =
      pathname === item.to || pathname.startsWith(`${item.to}/`);

    if (selfActive) {
      return true;
    }

    return (item.children ?? []).some(hasActivePath);
  };

  // primary 메뉴를 user 상태에 따라 동적으로 생성
  const primary = useMemo<MenuItem[]>(() => {
    const stationChildren = [
      { to: "/reviews", label: "Review" },
      { to: "/qna", label: "Q & A" },
    ];
    const routeChildren = []
    // 모바일이면 Story 항목을 숨김
    if (!isMobile) {
      routeChildren.push({ to: "/story", label: "Story" });
    }
    routeChildren.push({ to: "/stockist", label: "STOCKIST" });
    routeChildren.push({ to: "/logbook", label: "LOGBOOK" });

    const menuItems = [
      { to: "/new", label: "NEW" },
      {
        to: "/shop",
        label: "SHOP",
        children: [
          {
            to: "/original",
            label: "Mr.Trucker Original",
            children: [
              { to: "/cap", label: "CAP" },
              { to: "/acc", label: "ACC" },
            ],
          },
          {
            to: "/vintage",
            label: "The Old Truckers",
            children: [
              { to: "/vintage/cap", label: "CAP" },
              { to: "/vintage/shirt", label: "T-SHIRT" },
            ],
          },
        ],
      },
      {
        to: "/route",
        label: "ROUTE",
        children: routeChildren,
      },
      {
        to: "/station",
        label: "STATION",
        children: stationChildren,
      },
    ];

    // 관리자인 경우 Admin 메뉴를 독립 메뉴로 추가

    menuItems.push({ to: "/mygarage", label: "MY GARAGE" });

    if (user?.isAdmin) {
      menuItems.push({ to: "/admin", label: "ADMIN" });
    }

    return menuItems;
  }, [user?.isAdmin, isMobile]);

  useEffect(() => {
    fetch(`${SERVER_URL}/api/logo`)
      .then((res) => (res.ok ? res.json() : Promise.reject()))
      .then((data) => setLogoUrl(data?.url || null))
      .catch(() => setLogoUrl(null));
  }, []);

  return (
    <div className="min-h-screen flex flex-col bg-[#fffff0]" style={{ overflowX: "hidden" }}>
      <SitePopup disabled={pathname.startsWith("/admin")} />

      {/* Bangers font removed - replaced by local Beaver Punch */}

      {/* 전역 고정 버튼 */}
      <a
        href={INSTAGRAM_URL || "#"}
        target={INSTAGRAM_URL ? "_blank" : undefined}
        rel={INSTAGRAM_URL ? "noopener noreferrer" : undefined}
        className="fixed bottom-6 left-6 z-50 transition-transform transition-opacity hover:opacity-80 hover:scale-105 flex items-center"
        aria-label="Instagram"
        title="Instagram"
      >
        <span
          className="font-beaver text-white text-base md:text-2xl ml-5"
          style={{
            transform: "scaleY(1.3)",
            transformOrigin: "left bottom",
            letterSpacing: "2px",
            display: "inline-block",
            height: "fit-content",
          }}
        >
          INSTAGRAM
        </span>
      </a>

      <a
        href={KAKAO_URL || "#"}
        target={KAKAO_URL ? "_blank" : undefined}
        rel={KAKAO_URL ? "noopener noreferrer" : undefined}
        onClick={(e) => {
          if (!KAKAO_URL) {
            e.preventDefault();
            alert("카카오톡 주소가 설정되지 않았습니다. VITE_KAKAO_URL을 설정해주세요.");
          }
        }}
        className="fixed bottom-6 right-6 z-50 transition-transform transition-opacity hover:opacity-80 hover:scale-105 flex items-center"
        aria-label="KakaoTalk"
        title="KakaoTalk"
      >
        <img
          src={`${SERVER_URL}/uploads/cap/katalk.webp`}
          alt="KakaoTalk"
          className="w-12 h-12 md:w-16 md:h-16 object-contain mr-6"
          onError={(e) => {
            (e.currentTarget as HTMLImageElement).src = `${SERVER_URL}/uploads/cap/emptyload.webp`;
          }}
        />
        <span className="sr-only">KakaoTalk</span>
      </a>

      {/* 상단 네비게이션 */}
      <header className="w-full fixed top-0 z-50 bg-transparent">
        {/* ===== 상단 바 (공통) ===== */}
        <div className="w-full flex items-center justify-between px-4 md:px-6 py-3">
          {/* 데스크탑: 로고 (좌측) */}
          <Link to="/" className="hidden md:flex items-center">
            {safeSrc(logoUrl) ? (
              <img
                src={safeSrc(logoUrl)}
                alt="Hey Mr. Trucker Logo"
                className="h-10 md:h-24 object-contain"
                onError={(e) => {
                  (e.currentTarget as HTMLImageElement).src = `${SERVER_URL}/uploads/cap/homelogo.webp`;
                }}
              />
            ) : null}
          </Link>

          {/* 모바일: 로고 (가운데) */}
          <Link to="/" className="flex md:hidden items-center justify-center flex-1">
            {safeSrc(logoUrl) ? (
              <img
                src={safeSrc(logoUrl)}
                alt="Hey Mr. Trucker Logo"
                className="h-10 object-contain"
                onError={(e) => {
                  (e.currentTarget as HTMLImageElement).src = `${SERVER_URL}/uploads/cap/homelogo.webp`;
                }}
              />
            ) : null}
          </Link>

          {/* ===== 데스크탑 네비 (md 이상에서만 보임) ===== */}
          <nav
            className="hidden md:flex items-center mr-auto ml-12"
            style={{ gap: "40px" }}
          >
            {primary.map((item) => {
              const hasChildren = !!item.children?.length;
              const activeChild = hasChildren && hasActivePath(item);

              return (
                <div key={item.to} className="relative group">
                  {/* 하위 메뉴가 있으면 클릭 불가능한 span, 없으면 NavLink */}
                  {hasChildren ? (
                    <span
                      className={[
                        "transition-all duration-200 text-white cursor-default font-beaver",
                        activeChild ? "drop-shadow-[0_0_6px_#fff]" : "hover:text-gray-200",
                      ].join(" ")}
                      style={{
                        transform: "scaleY(1.3)",
                        letterSpacing: "2px",
                        fontSize: "30px",
                        display: "inline-block",
                        transformOrigin: "center",
                      }}
                    >
                      {item.label}
                    </span>
                  ) : (
                    <NavLink
                      to={item.to}
                      className={({ isActive }) =>
                        [
                          "transition-all duration-200",
                          isActive
                            ? "text-white drop-shadow-[0_0_6px_#fff]"
                            : "text-white hover:text-gray-200",
                        ].join(" ")
                      }
                      style={{
                        transform: "scaleY(1.3)",
                        transformOrigin: "center",
                        letterSpacing: "2px",
                        fontSize: "30px",
                        display: "inline-block",
                      }}
                    >
                      <span className="font-beaver">{item.label}</span>
                    </NavLink>
                  )}

                  {hasChildren && (
                    <>
                      {/* hover 브리지: 부모 메뉴와 속메뉴 사이 갭에서 hover 유지 */}
                      <div className="absolute left-1/2 -translate-x-1/2 top-full h-1 w-full hidden group-hover:block" />
                      <div
                        className="absolute left-1/2 -translate-x-1/2 top-full hidden group-hover:block"
                        style={{
                          minWidth: "180px",
                          background: "transparent",
                          boxShadow: "none",
                          padding: "2px 0",
                          borderRadius: "6px",
                          zIndex: 60,
                        }}
                      >
                        {(item as any).children.map((child: any) => (
                          <div
                            key={child.to}
                            className="relative group/submenu"
                          >
                            {child.children?.length ? (
                              <div
                                className="px-4 py-1 whitespace-nowrap text-center text-white"
                                style={{
                                  letterSpacing:
                                    child.label === "Mr.Trucker Original" ||
                                      child.label === "The Old Truckers"
                                      ? "0.5px"
                                      : "1px",
                                  fontSize:
                                    child.label === "Mr.Trucker Original" ||
                                      child.label === "The Old Truckers"
                                      ? "20px"
                                      : "26px",
                                  textShadow: "0 2px 10px rgba(0, 0, 0, 0.55)",
                                }}
                              >
                                <span className="font-beaver">{child.label}</span>
                              </div>
                            ) : (
                              <Link
                                to={child.to}
                                className="block px-4 py-1 whitespace-nowrap text-center"
                                style={{
                                  letterSpacing: "1px",
                                  fontSize: "26px",
                                  color: "white",
                                  textShadow: "0 2px 10px rgba(0, 0, 0, 0.55)",
                                }}
                              >
                                <span className="font-beaver">{child.label}</span>
                              </Link>
                            )}

                            {child.children?.length ? (
                              <div className="hidden pt-1 group-hover/submenu:block">
                                <div
                                  style={{
                                    minWidth: "190px",
                                    background: "transparent",
                                    boxShadow: "none",
                                    padding: "2px 0",
                                    borderRadius: "6px",
                                    zIndex: 70,
                                  }}
                                >
                                  {child.children.map((grandChild: MenuItem) => (
                                    <Link
                                      key={grandChild.to}
                                      to={grandChild.to}
                                      className="block px-4 py-1 whitespace-nowrap text-center"
                                      style={{
                                        letterSpacing: "0.5px",
                                        fontSize: "20px",
                                        color: "white",
                                        textShadow: "0 2px 10px rgba(0, 0, 0, 0.55)",
                                      }}
                                    >
                                      <span className="font-beaver">
                                        {grandChild.label}
                                      </span>
                                    </Link>
                                  ))}
                                </div>
                              </div>
                            ) : null}
                          </div>
                        ))}
                      </div>
                    </>
                  )}
                </div>
              );
            })}
          </nav>

          {/* ===== 데스크탑 우측 아이콘 (md 이상) ===== */}
          <div className="hidden md:flex items-center">
            <Link to="/mygarage" className="inline-block">
              <img
                src={`${SERVER_URL}/uploads/cap/${user ? "loginIcon" : "noLoginIcon"}.webp`}
                alt="User"
                className="w-15 h-15 opacity-90 cursor-pointer hover:opacity-100 transition-opacity"
                onError={(e) => {
                  (e.currentTarget as HTMLImageElement).src = `${SERVER_URL}/uploads/cap/emptyload.webp`;
                }}
              />
            </Link>
          </div>

          {/* ===== 모바일 좌측 영역 (md 미만) - 유저 아이콘 ===== */}
          <div className="flex items-center md:hidden absolute left-4">
            {/* 유저 아이콘 */}
            <Link to="/mygarage" className="inline-block">
              <img
                src={`${SERVER_URL}/uploads/cap/${user ? "loginIcon" : "noLoginIcon"}.webp`}
                alt="User"
                className="w-10 h-10 opacity-90 cursor-pointer hover:opacity-100 transition-opacity"
                onError={(e) => {
                  (e.currentTarget as HTMLImageElement).src = `${SERVER_URL}/uploads/cap/emptyload.webp`;
                }}
              />
            </Link>
          </div>

          {/* ===== 모바일 우측 영역 (md 미만) - 햄버거/X 버튼 ===== */}
          <div className="flex items-center md:hidden absolute right-4 z-50">
            {/* 햄버거/X 버튼 */}
            <button
              type="button"
              className="flex flex-col justify-center items-center w-9 h-9 border border-yellow-400 rounded-md bg-transparent relative z-50"
              onClick={() => setMobileMenuOpen((prev) => !prev)}
            >
              {mobileMenuOpen ? (
                // X 버튼
                <>
                  <span className="block w-5 h-0.5 bg-yellow-500 absolute transform rotate-45" />
                  <span className="block w-5 h-0.5 bg-yellow-500 absolute transform -rotate-45" />
                </>
              ) : (
                // 햄버거 버튼
                <>
                  <span className="block w-5 h-0.5 bg-yellow-500 mb-1" />
                  <span className="block w-5 h-0.5 bg-yellow-500 mb-1" />
                  <span className="block w-5 h-0.5 bg-yellow-500" />
                </>
              )}
            </button>
          </div>
        </div>

        {/* ===== 모바일: 오른쪽 오프캔버스 메뉴 (md 미만) ===== */}
        {mobileMenuOpen && (
          <>
            {/* 스크린 오버레이: 클릭 시 닫힘 */}
            <div
              className="fixed inset-0 bg-black/40 z-40 md:hidden"
              onClick={() => setMobileMenuOpen(false)}
            />

            {/* 오른쪽 패널 */}
            <aside className="fixed top-0 right-0 h-full w-50 md:hidden z-50 bg-transparent border-l border-yellow-200 shadow-md mt-20">
              <div className="h-full px-4 py-6 overflow-y-auto">
                {primary
                  .filter((it) => it.to !== "/new")
                  .map((item) => {
                    const hasChildren = !!item.children?.length;
                    const isExpanded = !!expandedItems[item.to];
                    const activeChild = hasChildren && hasActivePath(item);

                    return (
                      <div key={item.to} className="mb-3">
                        {hasChildren ? (
                          <button
                            type="button"
                            onClick={() => toggleExpanded(item.to)}
                            className={[
                              "w-full flex items-center justify-between text-white text-lg tracking-widest font-beaver px-2 py-2",
                              activeChild ? "font-semibold drop-shadow-[0_0_6px_#fff]" : "hover:text-gray-200",
                            ].join(" ")}
                          >
                            <span className="text-left">{item.label}</span>
                            <span className={`ml-3 transition-transform ${isExpanded ? "rotate-90" : "rotate-0"}`}>
                              ▶
                            </span>
                          </button>
                        ) : (
                          <NavLink
                            to={item.to}
                            onClick={() => setMobileMenuOpen(false)}
                            className={({ isActive }) =>
                              [
                                "block text-white text-lg tracking-widest px-2 py-2",
                                isActive ? "font-semibold" : "hover:text-gray-200",
                              ].join(" ")
                            }
                          >
                            {item.label}
                          </NavLink>
                        )}

                        {/* 하위 메뉴 (토글로 표시) */}
                        {hasChildren && isExpanded && (
                          <div className="pl-4 mt-2 space-y-1">
                            {item.children?.map((child) => {
                              const childKey = `${item.to}:${child.to}`;
                              const childExpanded = !!expandedItems[childKey];

                              if (child.children?.length) {
                                return (
                                  <div key={child.to}>
                                    <button
                                      type="button"
                                      onClick={() => toggleExpanded(childKey)}
                                      className={[
                                        "w-full text-left text-base text-white hover:text-gray-200",
                                        hasActivePath(child) ? "font-semibold" : "",
                                      ].join(" ")}
                                      style={{
                                        fontSize:
                                          child.label === "Mr.Trucker Original" ||
                                            child.label === "The Old Truckers"
                                            ? "14px"
                                            : "16px",
                                        letterSpacing:
                                          child.label === "Mr.Trucker Original" ||
                                            child.label === "The Old Truckers"
                                            ? "0.5px"
                                            : undefined,
                                      }}
                                    >
                                      <span className="font-beaver">{child.label}</span>
                                    </button>

                                    {childExpanded && (
                                      <div className="pl-4 mt-2 space-y-1">
                                        {child.children.map((grandChild: MenuItem) => (
                                          <NavLink
                                            key={grandChild.to}
                                            to={grandChild.to}
                                            onClick={() => setMobileMenuOpen(false)}
                                            className={({ isActive }) =>
                                              [
                                                "block text-white hover:text-gray-200",
                                                isActive ? "font-semibold" : "",
                                              ].join(" ")
                                            }
                                            style={{
                                              fontSize: "14px",
                                              letterSpacing: "0.5px",
                                            }}
                                          >
                                            <span className="font-beaver">
                                              {grandChild.label}
                                            </span>
                                          </NavLink>
                                        ))}
                                      </div>
                                    )}
                                  </div>
                                );
                              }

                              return (
                                <NavLink
                                  key={child.to}
                                  to={child.to}
                                  onClick={() => setMobileMenuOpen(false)}
                                  className={({ isActive }) =>
                                    [
                                      "block text-base text-white hover:text-gray-200",
                                      isActive ? "font-semibold" : "",
                                    ].join(" ")
                                  }
                                >
                                  <span className="font-beaver">{child.label}</span>
                                </NavLink>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    );
                  })}
              </div>
            </aside>
          </>
        )}
      </header>

      <main className="flex-1">
        {/* Outlet 래퍼: 빈 src 문제 없음 */}

        <Outlet />

      </main>

      <Footer />
    </div>
  );
}
