import { useEffect, useState } from "react";
import { useAuth } from "../auth/useAuth";
import { useNavigate } from "react-router-dom";
import OrderManagement from "./admin/OrderManagement";
import ProductManagement from "./admin/ProductManagement";
import UserManagement from "./admin/UserManagement";
import Coupon from "./admin/Coupon";
import StoryManagement from "./admin/StoryManagement";
import PopupManagement from "./admin/PopupManagement";

export default function AdminPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [ordersOpen, setOrdersOpen] = useState<boolean>(false);
  const [productsOpen, setProductsOpen] = useState<boolean>(false);
  const [usersOpen, setUsersOpen] = useState<boolean>(false);
  const [couponOpen, setCouponOpen] = useState<boolean>(false);
  const [storyOpen, setStoryOpen] = useState<boolean>(false);
  const [popupOpen, setPopupOpen] = useState<boolean>(false);

  useEffect(() => {
    // 토큰이 없으면 로그인 페이지로 리다이렉트
    const token = localStorage.getItem("access_token");
    if (!token) {
      alert("로그인이 필요합니다.");
      navigate("/login");
      return;
    }

    // 관리자가 아니면 홈으로 리다이렉트
    if (!user?.isAdmin) {
      alert("관리자만 접근할 수 있습니다.");
      navigate("/");
    }
  }, [user, navigate]);

  if (!user?.isAdmin) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-400">
      <div className="w-[900px] mx-auto py-32">
      <h1 className="text-2xl font-bold mb-6">관리자 페이지</h1>

      {/* 주문 관리 */}
      <OrderManagement 
        isOpen={ordersOpen} 
        onToggle={() => setOrdersOpen(v => !v)} 
      />

      {/* 관리자 기능들 그리드 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* 상품 관리 */}
        <ProductManagement 
          isOpen={productsOpen} 
          onToggle={() => setProductsOpen(v => !v)} 
        />

        {/* 사용자 관리 */}
        <UserManagement 
          isOpen={usersOpen} 
          onToggle={() => setUsersOpen(v => !v)} 
        />
        
        {/* 쿠폰 관리 */}
        <Coupon 
          isOpen={couponOpen} 
          onToggle={() => setCouponOpen(v => !v)} 
        />

        {/* 스토리 관리 */}
        <StoryManagement
          isOpen={storyOpen}
          onToggle={() => setStoryOpen((v) => !v)}
        />

        {/* 팝업 관리 */}
        <PopupManagement
          isOpen={popupOpen}
          onToggle={() => setPopupOpen((v) => !v)}
        />
      </div>
      </div>
    </div>
  );
}
