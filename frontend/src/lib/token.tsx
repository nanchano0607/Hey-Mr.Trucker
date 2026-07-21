export const getAccessToken = () => localStorage.getItem('access_token');
export const setAccessToken = (t: string) => localStorage.setItem('access_token', t);
export const clearAccessToken = () => localStorage.removeItem('access_token');
