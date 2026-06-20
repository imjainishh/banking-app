import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import api from '../api'

export default function DashboardPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [opening, setOpening] = useState(false)
  const [error, setError] = useState('')

  // Load accounts on page load
  useEffect(() => {
    fetchAccounts()
  }, [])

  const fetchAccounts = async () => {
    try {
      const res = await api.get('/accounts')
      setAccounts(res.data)
    } catch {
      setError('Failed to load accounts')
    } finally {
      setLoading(false)
    }
  }

  const openAccount = async (type) => {
    setOpening(true)
    try {
      await api.post('/accounts', { accountType: type })
      fetchAccounts() // refresh list
    } catch {
      setError('Failed to open account')
    } finally {
      setOpening(false)
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div style={styles.page}>

      {/* Header */}
      <div style={styles.header}>
        <h1 style={styles.logo}>🏦 BankApp</h1>
        <div style={styles.headerRight}>
          <span style={styles.welcome}>Hello, {user?.fullName}</span>
          <button style={styles.logoutBtn} onClick={handleLogout}>Logout</button>
        </div>
      </div>

      <div style={styles.content}>
        <h2 style={styles.sectionTitle}>My Accounts</h2>

        {error && <p style={styles.error}>{error}</p>}

        {/* Open new account buttons */}
        <div style={styles.openRow}>
          <button
            style={styles.openBtn}
            onClick={() => openAccount('SAVINGS')}
            disabled={opening}
          >
            + Open Savings Account
          </button>
          <button
            style={{ ...styles.openBtn, background: '#7b1fa2' }}
            onClick={() => openAccount('CURRENT')}
            disabled={opening}
          >
            + Open Current Account
          </button>
        </div>

        {/* Account cards */}
        {loading ? (
          <p style={styles.empty}>Loading...</p>
        ) : accounts.length === 0 ? (
          <p style={styles.empty}>No accounts yet. Open one above!</p>
        ) : (
          <div style={styles.grid}>
            {accounts.map(account => (
              <div
                key={account.id}
                style={{
                  ...styles.card,
                  borderTop: `4px solid ${account.accountType === 'SAVINGS' ? '#1a73e8' : '#7b1fa2'}`
                }}
                onClick={() => navigate(`/accounts/${account.id}`)}
              >
                <div style={styles.cardType}>{account.accountType}</div>
                <div style={styles.cardNumber}>{account.accountNumber}</div>
                <div style={styles.cardBalance}>
                  £{parseFloat(account.balance).toFixed(2)}
                </div>
                <div style={styles.cardStatus}>{account.status}</div>
                <div style={styles.cardHint}>Click to view →</div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

const styles = {
  page: { minHeight: '100vh', background: '#f0f2f5' },
  header: { background: 'white', padding: '1rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' },
  logo: { fontSize: '1.5rem', margin: 0 },
  headerRight: { display: 'flex', alignItems: 'center', gap: '1rem' },
  welcome: { color: '#555' },
  logoutBtn: { padding: '0.4rem 1rem', background: 'transparent', border: '1px solid #ddd', borderRadius: '8px', cursor: 'pointer' },
  content: { maxWidth: '900px', margin: '0 auto', padding: '2rem' },
  sectionTitle: { marginBottom: '1rem' },
  openRow: { display: 'flex', gap: '1rem', marginBottom: '2rem' },
  openBtn: { padding: '0.75rem 1.5rem', background: '#1a73e8', color: 'white', border: 'none', borderRadius: '8px', cursor: 'pointer', fontSize: '0.95rem' },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: '1rem' },
  card: { background: 'white', borderRadius: '12px', padding: '1.5rem', cursor: 'pointer', boxShadow: '0 1px 4px rgba(0,0,0,0.08)', transition: 'transform 0.15s' },
  cardType: { fontSize: '0.8rem', fontWeight: 600, color: '#888', letterSpacing: '0.05em', marginBottom: '0.5rem' },
  cardNumber: { fontSize: '0.95rem', color: '#333', marginBottom: '1rem', fontFamily: 'monospace' },
  cardBalance: { fontSize: '2rem', fontWeight: 700, color: '#1a1a1a', marginBottom: '0.5rem' },
  cardStatus: { fontSize: '0.8rem', color: '#4caf50', fontWeight: 600 },
  cardHint: { fontSize: '0.8rem', color: '#aaa', marginTop: '1rem' },
  error: { color: '#d32f2f', background: '#fdecea', padding: '0.75rem', borderRadius: '8px', marginBottom: '1rem' },
  empty: { color: '#888', textAlign: 'center', marginTop: '3rem' }
}