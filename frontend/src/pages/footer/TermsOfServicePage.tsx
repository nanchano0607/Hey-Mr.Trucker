import { Link } from "react-router-dom";

export default function TermsOfServicePage() {
  return (
    <div className="font-sans min-h-screen bg-gray-500 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto bg-gray-200 rounded-lg shadow-md p-8 mt-16">
        <h1 className="text-3xl font-bold mb-8">이용약관 (전자상거래 이용약관)</h1>
        
        {/* 환불정책 바로가기 버튼 */}
        <div className="mb-6 p-4 bg-blue-50 border-2 border-blue-300 rounded-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="font-semibold text-gray-800 mb-1">💳 결제 전 필독</p>
              <p className="text-sm text-gray-600">청약철회, 환불, 배송비 등 상세 정책을 확인하세요</p>
            </div>
            <Link
              to="/policy/refund"
              className="px-4 py-2 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors whitespace-nowrap"
            >
              청약철회 및 환불정책 보기 →
            </Link>
          </div>
        </div>

        <div className="prose prose-sm max-w-none space-y-6">
          <section>
            <p className="text-gray-700">
              본 약관은 HEY! MR. TRUCKER(이하 "회사")가 운영하는 온라인 쇼핑몰에서 제공하는 전자상거래 관련 서비스의
              이용과 관련하여 회사와 이용자의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">제1조 (목적)</h2>
            <p className="text-gray-700">
              이 약관은 회사가 제공하는 전자상거래 서비스 이용에 관한 조건 및 절차, 회사와 이용자 간의 권리·의무 및
              책임사항을 규정함을 목적으로 합니다.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">제2조 (정의)</h2>
            <div className="text-gray-700 space-y-3">
              <p>
                <strong>"사이트"란</strong> 회사가 상품을 이용자에게 제공하기 위하여 운영하는 온라인 쇼핑몰을 의미합니다.
              </p>
              <p>
                <strong>"이용자"란</strong> 사이트에 접속하여 본 약관에 따라 회사가 제공하는 서비스를 이용하는 자를 말합니다.
              </p>
              <p>
                <strong>"상품"이란</strong> 회사가 사이트를 통해 판매하는 모자 및 관련 패션 잡화 일체를 의미합니다.
              </p>
            </div>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">제3조 (서비스의 제공)</h2>
            <p className="font-semibold mb-2">회사는 다음과 같은 서비스를 제공합니다.</p>
            <ul className="list-disc list-inside text-gray-700 space-y-2">
              <li>상품 정보 제공 및 구매계약 체결</li>
              <li>결제 서비스 제공</li>
              <li>배송 및 고객 응대 서비스</li>
            </ul>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">제4조 (결제)</h2>
            <div className="text-gray-700 space-y-3">
              <p>이용자는 회사가 제공하는 결제수단을 통해 상품 대금을 결제할 수 있습니다.</p>
              <p>
                결제는 전자결제대행사(PG)를 통해 이루어지며, 회사는 이용자의 결제 정보를 직접 저장하지 않습니다.
              </p>
            </div>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">제5조 (청약철회 및 환불)</h2>
            <div className="text-gray-700 space-y-3">
              <p>이용자는 상품 수령일로부터 7일 이내에 청약철회를 할 수 있습니다.</p>
              <p className="font-semibold">다음 각 호의 경우에는 청약철회가 제한될 수 있습니다.</p>
              <ul className="list-disc list-inside space-y-1">
                <li>이용자의 책임으로 상품이 훼손된 경우</li>
                <li>사용 또는 소비로 상품 가치가 현저히 감소한 경우</li>
              </ul>
              <p>환불은 결제 수단에 따라 영업일 기준 일정 기간이 소요될 수 있습니다.</p>
            </div>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">제6조 (회사의 책임 제한)</h2>
            <p className="text-gray-700">
              회사는 천재지변, 시스템 장애 등 불가항력적 사유로 인한 서비스 중단에 대해 책임을 지지 않습니다.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">제7조 (분쟁 해결)</h2>
            <p className="text-gray-700">
              회사와 이용자 간 분쟁이 발생할 경우, 상호 성실히 협의하여 해결하며, 협의가 어려운 경우 관계 법령에 따릅니다.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">제8조 (약관의 변경)</h2>
            <p className="text-gray-700">
              본 약관은 관련 법령을 위반하지 않는 범위에서 변경될 수 있으며, 변경 시 사이트를 통해 공지합니다.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
