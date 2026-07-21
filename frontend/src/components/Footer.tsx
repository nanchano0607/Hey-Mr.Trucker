import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="font-sans bg-[#171f30] text-gray-300 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="border-t border-gray-700 pt-8">
          <div className="flex flex-wrap justify-center gap-4 mb-6">
            <Link
              to="/terms-of-service"
              className="hover:text-white transition-colors"
            >
              이용약관
            </Link>
            <span className="text-gray-600">|</span>
            <Link
              to="/policy/refund"
              className="hover:text-white transition-colors"
            >
              환불정책
            </Link>
            <span className="text-gray-600">|</span>
            <Link
              to="/privacy-policy"
              className="hover:text-white transition-colors"
            >
              개인정보처리방침
            </Link>
          </div>

          <div className="text-center text-sm text-gray-500 space-y-2">
            <Link
              to="/company-info"
              className="block hover:text-white transition-colors cursor-pointer"
            >
              <p>&copy; 2026 HEY! MR. TRUCKER. All rights reserved.</p>
              <p className="text-xs">경기도 수원시 팔달구 효원로12번길 11-15, 1층 101호 | 대표: 김건호</p>
              <p className="text-xs">gss0227@naver.com,  070-8098-2611</p>
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
