import { Routes, Route, Navigate } from "react-router-dom";
import RootLayout from "./layouts/RootLayout";
import HomePage from "./pages/HomePage";

import StoryPage from "./pages/route/StoryPage";
import StockistPage from "./pages/route/StockistPage";
import LoginPage from "./pages/login/LoginPage";
import CartPage from "./pages/mygarage/CartPage";

import MyGaragePage from "./pages/mygarage/MyGaragePage";
import LicensePage from "./pages/mygarage/LicensePage";
import { AuthProvider } from "./auth/AuthProvider";
import ProtectedRoute from "./auth/ProtectedRoute";


import CapPage from "./pages/product/CapPage";
import AccPage from "./pages/product/AccPage";
import VintagePage from "./pages/product/VintagePage";
import LogBookPage from "./pages/route/LogBookPage";
import ProductDetailPage from "./pages/product/ProductDetailPage";
import NewPage from "./pages/NewPage";

import RegisterPage from "./pages/admin/LegisterPage";
import SignUp from "./pages/login/SignUp";
import { CartProvider } from "./auth/CartContext";
import BuyPage from "./pages/buy/BuyPage";
import PaymentPage from "./pages/buy/PaymentPage";
import SignupPhone from "./pages/login/SignupPhone";
import AddressPage from "./pages/mygarage/AddressPage";
import SuccessPage from "./pages/buy/SuccessPage";
import FailPage from "./pages/buy/FailPage";
import OrderPage from "./pages/mygarage/OrderPage";
import OrderDetailPage from "./pages/mygarage/OrderDetailPage";
import PointsPage from "./pages/mygarage/PointsPage";
import AdminPage from "./pages/AdminPage";
import ReviewWritePage from "./pages/ReviewWritePage";
import ReviewPage from "./pages/station/ReviewPage";
import QnAPage from "./pages/station/QnAPage";
import QnADetailPage from "./pages/station/QnADetailPage";
import WriteNotice from "./pages/station/WriteNotice";
import TermsOfServicePage from "./pages/footer/TermsOfServicePage";
import PrivacyPolicyPage from "./pages/footer/PrivacyPolicyPage";
import CompanyInfoPage from "./pages/footer/CompanyInfoPage";
import RefundPolicyPage from "./pages/footer/RefundPolicyPage";

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <Routes>
          <Route element={<RootLayout />}>
            <Route index element={<HomePage />} />
            <Route path="/new" element={<NewPage />} />
            <Route path="/cap" element={<CapPage />} />
            <Route path="/acc" element={<AccPage />} />
            <Route path="/vintage/:category" element={<VintagePage />} />
            <Route path="/product/:id" element={<ProductDetailPage />} />
            <Route path="/logbook" element={<LogBookPage />} />
            <Route path="/stockist" element={<StockistPage />} />
            
            <Route path="/story" element={<StoryPage />} />
            <Route path="/register" element={<RegisterPage />} /> {/* 등록 페이지 추가 */}
            
            <Route
              path="/cart"
              element={
                <ProtectedRoute>
                  <CartPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/mygarage"
              element={
                <ProtectedRoute>
                  <MyGaragePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/address"
              element={
                <ProtectedRoute>
                  <AddressPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/license"
              element={
                <ProtectedRoute>
                  <LicensePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/order"
              element={
                <ProtectedRoute>
                  <OrderPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/order/:id"
              element={
                <ProtectedRoute>
                  <OrderDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/points"
              element={
                <ProtectedRoute>
                  <PointsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin"
              element={
                <ProtectedRoute>
                  <AdminPage />
                </ProtectedRoute>
              }
            />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/login/success" element={<LoginPage success />} />
            <Route path="/complete-signup" element={<SignupPhone />} />
            <Route path="/signup" element={<SignUp />} />
            <Route
              path="/buy"
              element={
                <ProtectedRoute>
                  <BuyPage />
                </ProtectedRoute>
              }
            />
            <Route path="/payment" element={<PaymentPage />} />
            <Route 
              path="/review/write"
              element={
                <ProtectedRoute>
                  <ReviewWritePage />
                </ProtectedRoute>
              }
            />
            <Route path="/reviews" element={<ReviewPage />} />
            <Route path="/qna" element={<QnAPage />} />
            <Route path="/qna/:id" element={<QnADetailPage />} />
            <Route
              path="/qna/write"
              element={
                <ProtectedRoute>
                  <WriteNotice />
                </ProtectedRoute>
              }
            />
            <Route path="/success" element={<SuccessPage />} /> {/* 결제 성공 페이지 */}
            <Route path="/fail" element={<FailPage />} /> {/* 결제 실패 페이지 */}
            <Route path="/terms-of-service" element={<TermsOfServicePage />} />
            <Route path="/policy/refund" element={<RefundPolicyPage />} />
            <Route path="/privacy-policy" element={<PrivacyPolicyPage />} />
            <Route path="/company-info" element={<CompanyInfoPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </CartProvider>
    </AuthProvider>
  );
}
