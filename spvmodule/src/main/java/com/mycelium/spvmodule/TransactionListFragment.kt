package com.mycelium.spvmodule

import android.app.ListFragment
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import com.mycelium.spvmodule.Constants.Companion.TAG

import com.mycelium.spvmodule.providers.BlockchainContract

class TransactionListFragment : ListFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val context = activity
        val cr = context.contentResolver
        val uri = BlockchainContract.Transaction.CONTENT_URI(context.packageName)
        Log.d(TAG, "BlockchainContract.Transaction URI is ${uri}")
        val cursor = cr.query(BlockchainContract.Transaction.CONTENT_URI(context.packageName), TransactionsAdapter.fromColumns, null, null, null)

        cr.registerContentObserver(BlockchainContract.Transaction.CONTENT_URI(context.packageName), true, object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                activity.runOnUiThread {
                    // val cursor2 = cr.query(BlockchainContract.Transaction.CONTENT_URI(context.packageName), TransactionsAdapter.fromColumns, null, null, null)
                    // adapter.swapCursor(cursor);
                    Log.d(TAG, "We actually got notified of a transactions change.")
                }
            }

            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }
        })
        // cursor.setNotificationUri(cr, Transactions.CONTENT_URI);
        listAdapter = TransactionsAdapter(context, cursor)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private class TransactionsAdapter internal constructor(context: Context, cursor: Cursor) : SimpleCursorAdapter(context, R.layout.transaction_row, cursor, TransactionListFragment.TransactionsAdapter.fromColumns, TransactionListFragment.TransactionsAdapter.toViews, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {
        private val layoutInflater = LayoutInflater.from(context)
        private val txid = cursor.getColumnIndexOrThrow(BlockchainContract.Transaction.TRANSACTION_ID)
        private val includedInBlock = cursor.getColumnIndexOrThrow(BlockchainContract.Transaction.INCLUDED_IN_BLOCK)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var cv = convertView
            if (cursor.moveToPosition(position)) {
                val viewHolder: ViewHolder

                if (cv == null) {
                    cv = layoutInflater.inflate(R.layout.transaction_row,
                            parent, false)
                    viewHolder = ViewHolder(cv)
                    cv!!.tag = viewHolder
                } else {
                    viewHolder = cv.tag as ViewHolder
                }

                viewHolder.txid.text = cursor.getString(txid)
                viewHolder.includedInBlock.text = (lastBlockSeenHeight - cursor.getLong(includedInBlock)).toString()
            }
            Log.d(TAG, "getView at position $position. TXID is ${cursor.getString(txid)}.")
            return cv
        }

        private inner class ViewHolder internal constructor(v: View) {
            internal var txid: TextView = v.findViewById(R.id.tvTXID) as TextView
            internal var includedInBlock: TextView = v.findViewById(R.id.tvBlockheight) as TextView
        }

        companion object {
            internal var fromColumns = arrayOf(BlockchainContract.Transaction.TRANSACTION_ID, BlockchainContract.Transaction.INCLUDED_IN_BLOCK)
            private val toViews = intArrayOf(R.id.tvTXID, R.id.tvBlockheight)
        }
    }

    companion object {
        // TODO: 9/15/16 this is definitely no long term solution. Update on new block
        private val lastBlockSeenHeight = SpvModuleApplication.getWallet().lastBlockSeenHeight
    }
}