import React, { useState } from "react";
import { getAccessToken } from "../../lib/token";
import { API_BASE_URL } from "../../config/apiBase";

const COUPON_TYPES = [
  { value: "PERCENTAGE", label: "퍼센트 할인" },
  { value: "AMOUNT", label: "금액 할인" }
];

interface CouponAdminProps {
  isOpen: boolean;
  onToggle: () => void;
}

export default function Coupon({ isOpen, onToggle }: CouponAdminProps) {
  const [form, setForm] = useState({
    name: "",
    code: "",
    type: "PERCENTAGE",
    discountValue: "",
    minOrderAmount: "",
    maxDiscountAmount: "",
    description: ""
  });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [coupons, setCoupons] = useState<any[]>([]);
  const [listLoading, setListLoading] = useState(false);
  const [showList, setShowList] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    let newValue: string | number | boolean = value;
    if (type === "checkbox") {
      newValue = (e.target as HTMLInputElement).checked;
    }
    setForm((prev) => ({
      ...prev,
      [name]: newValue
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const token = getAccessToken();
      const url = `${API_BASE_URL}/api/coupons/admin`;
      const res = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: JSON.stringify(form),
        credentials: "include"
      });

      if (!res.ok) {
        const errBody = await res.json().catch(() => ({}));
        throw new Error(errBody?.error || `HTTP ${res.status}`);
      }

      const data = await res.json();
      setResult(data);
      // 새로 생성하면 목록이 열려있을 때만 갱신
      if (showList) await fetchCoupons();
    } catch (err: any) {
      setError(err?.message || "쿠폰 생성 실패");
    } finally {
      setLoading(false);
    }
  };

  const fetchCoupons = async () => {
    setListLoading(true);
    setError("");
    try {
      const token = getAccessToken();
      const url = `${API_BASE_URL}/api/coupons/admin/all`;
      const res = await fetch(url, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        credentials: "include"
      });

      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body?.error || `HTTP ${res.status}`);
      }

      const data = await res.json();
      setCoupons(Array.isArray(data) ? data : []);
    } catch (e: any) {
      console.error('쿠폰 목록 조회 실패', e);
      setError(e?.message || '쿠폰 목록을 불러오지 못했습니다.');
    } finally {
      setListLoading(false);
    }
  };

  return (
    <div className="border p-4 rounded font-sans">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">쿠폰 관리</h2>
        <button
          onClick={onToggle}
          className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
        >
          {isOpen ? "접기" : "펼치기"}
        </button>
      </div>

      {isOpen && (
        <div className="mt-4">
            <div className="flex items-center justify-between mb-4">
            <div />
            <div className="flex gap-2">
              <button
                onClick={async () => {
                  if (showList) {
                    setShowList(false);
                    return;
                  }
                  // show list: fetch then show
                  await fetchCoupons();
                  setShowList(true);
                }}
                className="px-3 py-1 border rounded text-sm bg-green-300 hover:bg-green-200"
              >
                {listLoading ? '조회 중...' : showList ? '목록 숨기기' : '쿠폰 목록 조회'}
              </button>
            </div>
          </div>

          {/* 쿠폰 목록 */}
          {showList && coupons.length > 0 && (
            <div className="mb-4">
              <h3 className="font-semibold mb-2">전체 쿠폰 목록</h3>
              <div className="overflow-auto max-h-56 border rounded bg-white p-2">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-gray-600">
                      <th className="p-1">ID</th>
                      <th className="p-1">이름</th>
                      <th className="p-1">코드</th>
                      <th className="p-1">타입</th>
                      <th className="p-1">값</th>
                      <th className="p-1">유효기간</th>
                    </tr>
                  </thead>
                  <tbody>
                    {coupons.map((c) => (
                      <tr key={c.id} className="border-t">
                        <td className="p-1">{c.id}</td>
                        <td className="p-1">{c.name}</td>
                        <td className="p-1">{c.code}</td>
                        <td className="p-1">{c.type}</td>
                        <td className="p-1">{c.discountValue ?? '-'}{c.type === 'PERCENTAGE' ? '%' : '원'}</td>
                        <td className="p-1">{c.validUntil ? new Date(c.validUntil).toLocaleString() : '무기한'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
          <div className="flex items-center justify-end gap-2 mb-4">
            <button
              onClick={() => {
                setForm({
                  name: "",
                  code: "",
                  type: "PERCENTAGE",
                  discountValue: "",
                  minOrderAmount: "",
                  maxDiscountAmount: "",
                  description: ""
                });
                setResult(null);
                setError("");
              }}
              className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
            >
              초기화
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
        <input name="name" value={form.name} onChange={handleChange} placeholder="쿠폰명" className="w-full p-2 border rounded" required />
        <input name="code" value={form.code} onChange={handleChange} placeholder="쿠폰 코드" className="w-full p-2 border rounded" required />
            <select name="type" value={form.type} onChange={handleChange} className="w-full p-2 border rounded">
              {COUPON_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
            <input name="discountValue" type="number" value={form.discountValue} onChange={handleChange} placeholder="할인값" className="w-full p-2 border rounded" required />
            <input name="minOrderAmount" type="number" value={form.minOrderAmount} onChange={handleChange} placeholder="최소 주문금액" className="w-full p-2 border rounded" />
            <input name="maxDiscountAmount" type="number" value={form.maxDiscountAmount} onChange={handleChange} placeholder="최대 할인금액" className="w-full p-2 border rounded" />
            <textarea name="description" value={form.description} onChange={handleChange} placeholder="설명" className="w-full p-2 border rounded" />
            <button type="submit" className="w-full py-2 bg-blue-600 text-white rounded font-bold" disabled={loading}>
              {loading ? "생성 중..." : "쿠폰 생성"}
            </button>
          </form>

          {result && (
            <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded">
              <div className="font-bold text-green-700">쿠폰 생성 성공!</div>
              <pre className="text-xs mt-2">{JSON.stringify(result, null, 2)}</pre>
            </div>
          )}
          {error && (
            <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded text-red-700">{error}</div>
          )}
        </div>
      )}
    </div>
  );
}