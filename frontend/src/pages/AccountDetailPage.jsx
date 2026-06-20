import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api'

export default function AccountDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [balance, setBalance] = useState(null)
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(null) // 'deposit' | 'withdraw' | 'transfer'
  const [amount, setAmount] = useState('')
  const [targetAccount, setTargetAccount] = useState('')
  const [description, setDescription] = useState('')
  const [actionError, setActionError] = useState('')
  const [actionLoading, setActionLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [id])

  const fetchData = async () => {
    try {
      const [balRes, txRes] = await Promise.all([
        api.get(`/accounts/${id}/balance`),
        api.get(`/accounts/${id}/transactions`)
      ])
      setBalance(balRes.data)
      setTransactions(txRes.data)
    } catch {
      // handled below
    } finally {
      setLoading(false)
    }
  }

  const handleAction = async () => {
    setActionError('')
    setActionLoading(true)
    try {
      const body = {
        accountId: parseInt(id),
        amount: parseFloat(amount),
        description,
        ...(modal === 'transfer' && { targetAccountNumber: targetAccount })
      }
      const endpoint = `/transactions/${modal}`
      await api.post(endpoint, body)
      setModal(null)
      setAmount('')
      setDescription('')
      setTargetAccount('')
      fetchData() // refresh balance + history
    } catch (err) {
      setActionError(err.response?.data?.message || 'Action failed')
    } finally {
      setActionLoading(false)
    }
  }

  const typeColor = (type) => {
    if (type === 'DEPOSIT' || type === 'TRANSFER_IN') return '#2e7d32'
    return '#c62828'
  }

  const typeSign = (type) => {
    if (type === 'DEPOSIT' || type === 'TRANSFER_IN') return '+'
    return '-'
  }

  return (
    <div style={styles.page}>

      {/* Header */}
      <div style={styles.header}>
        <button style={styles.back} onClick={() => navigate('/dashboard')}>
          ← Back
        </button>
        <h1 style={styles.logo}>🏦 BankApp</h1>
      </div>

      <div style={styles.content}>

        {/* Balance */}
        <div style={styles.balanceCard}>
          <p style={styles.balanceLabel}>Current Balance</p>
          <p style={styles.balanceAmount}>
            {loading ? '...' : `£${parseFloat(balance).toFixed(2)}`}
          </p>

          {/* Action buttons */}
          <div style={styles.actionRow}>
            <button style={styles.actionBtn} onClick={() => setModal('deposit')}>
              Deposit
            </button>
            <button style={{ ...styles.actionBtn, background: '#e53935' }} onClick={() => setModal('withdraw')}>
              Withdraw
            </button>
            <button style={{ ...styles.actionBtn, background: '#7b1fa2' }} onClick={() => setModal('transfer')}>
              Transfer
            </button>
          </div>
        </div>

        {/* Transaction history */}
        <h2 style={styles.sectionTitle}>Transaction History</h2>
        {transactions.length === 0 ? (
          <p style={styles.empty}>No transactions yet.</p>
        ) : (
          <div style={styles.txList}>
            {transactions.map(tx => (
              <div key={tx.id} style={styles.txRow}>
                <div>
                  <p style={styles.txType}>{tx.type.replace('_', ' ')}</p>
                  <p style={styles.txDesc}>{tx.description || '—'}</p>
                  <p style={styles.txDate}>
                    {new Date(tx.createdAt).toLocaleString()}
                  </p>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <p style={{ ...styles.txAmount, color: typeColor(tx.type) }}>
                    {typeSign(tx.type)}£{parseFloat(tx.amount).toFixed(2)}
                  </p>
                  <p style={styles.txBalance}>
                    Balance: £{parseFloat(tx.balanceAfter).toFixed(2)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal */}
      {modal && (
        <div style={styles.overlay}>
          <div style={styles.modal}>
            <h3 style={{ marginBottom: '1rem', textTransform: 'capitalize' }}>
              {modal}
            </h3>

            {actionError && <p style={styles.error}>{actionError}</p>}

            <input
              style={styles.input}
              type="number"
              placeholder="Amount (£)"
              value={amount}
              onChange={e => setAmount(e.target.value)}
              min="0.01"
              step="0.01"
            />

            {modal === 'transfer' && (
              <input
                style={styles.input}
                type="text"
                placeholder="Target account number (ACC-XXXXXX)"
                value={targetAccount}
                onChange={e => setTargetAccount(e.target.value)}
              />
            )}

            <input
              style={styles.input}
              type="text"
              placeholder="Description (optional)"
              value={description}
              onChange={e => setDescription(e.target.value)}
            />

            <div style={styles.modalBtns}>
              <button
                style={styles.actionBtn}
                onClick={handleAction}
                disabled={actionLoading || !amount}
              >
                {actionLoading ? 'Processing...' : 'Confirm'}
              </button>
              <button
                style={{ ...styles.actionBtn, background: '#888' }}
                onClick={() => { setModal(null); setActionError('') }}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

const styles = {
  page: { minHeight: '100vh', background: '#f0f2f5' },
  header: { background: 'white', padding: '1rem 2rem', display: 'flex', alignItems: 'center', gap: '1rem', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' },
  back: { background: 'transparent', border: '1px solid #ddd', borderRadius: '8px', padding: '0.4rem 0.9rem', cursor: 'pointer' },
  logo: { fontSize: '1.5rem', margin: 0 },
  content: { maxWidth: '700px', margin: '0 auto', padding: '2rem' },
  balanceCard: { background: 'white', borderRadius: '12px', padding: '2rem', marginBottom: '2rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)', textAlign: 'center' },
  balanceLabel: { color: '#888', marginBottom: '0.5rem' },
  balanceAmount: { fontSize: '3rem', fontWeight: 700, marginBottom: '1.5rem' },
  actionRow: { display: 'flex', gap: '1rem', justifyContent: 'center' },
  actionBtn: { padding: '0.6rem 1.25rem', background: '#1a73e8', color: 'white', border: 'none', borderRadius: '8px', cursor: 'pointer', fontSize: '0.95rem' },
  sectionTitle: { marginBottom: '1rem' },
  txList: { background: 'white', borderRadius: '12px', overflow: 'hidden', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' },
  txRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem 1.5rem', borderBottom: '1px solid #f0f0f0' },
  txType: { fontWeight: 600, marginBottom: '2px' },
  txDesc: { color: '#888', fontSize: '0.85rem', marginBottom: '2px' },
  txDate: { color: '#aaa', fontSize: '0.8rem' },
  txAmount: { fontWeight: 700, fontSize: '1.1rem' },
  txBalance: { color: '#aaa', fontSize: '0.8rem' },
  empty: { color: '#888', textAlign: 'center', marginTop: '2rem' },
  overlay: { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 },
  modal: { background: 'white', borderRadius: '12px', padding: '2rem', width: '100%', maxWidth: '400px' },
  input: { width: '100%', padding: '0.75rem', marginBottom: '1rem', border: '1px solid #ddd', borderRadius: '8px', fontSize: '1rem', boxSizing: 'border-box' },
  modalBtns: { display: 'flex', gap: '1rem' },
  error: { color: '#d32f2f', background: '#fdecea', padding: '0.75rem', borderRadius: '8px', marginBottom: '1rem' }
}