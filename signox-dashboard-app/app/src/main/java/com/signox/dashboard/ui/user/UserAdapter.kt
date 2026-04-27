package com.signox.dashboard.ui.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.R
import com.signox.dashboard.data.model.UserManagement
import com.signox.dashboard.databinding.ItemUserCardBinding

class UserAdapter(
    private val onUserClick: (UserManagement) -> Unit,
    private val onMoreClick: (UserManagement, View) -> Unit
) : ListAdapter<UserManagement, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserManagement) {
            binding.apply {
                tvEmail.text = user.email
                chipRole.text = user.role.name.replace("_", " ")
                
                // Status chip
                chipStatus.text = if (user.isActive) "Active" else "Inactive"
                chipStatus.setChipBackgroundColorResource(
                    if (user.isActive) R.color.status_online else R.color.status_offline
                )
                
                // Company name for CLIENT_ADMIN
                if (user.clientProfile != null) {
                    tvCompany.visibility = View.VISIBLE
                    tvCompany.text = user.clientProfile.companyName
                } else {
                    tvCompany.visibility = View.GONE
                }
                
                // Display count
                if (user.displayCount != null && user.displayCount > 0) {
                    tvDisplayCount.visibility = View.VISIBLE
                    tvDisplayCount.text = "${user.displayCount} display(s)"
                } else {
                    tvDisplayCount.visibility = View.GONE
                }
                
                // Staff role chip
                if (user.staffRole != null) {
                    chipRole.text = "${user.role.name} - ${user.staffRole.name.replace("_", " ")}"
                }
                
                root.setOnClickListener {
                    onUserClick(user)
                }
                
                btnMore.setOnClickListener {
                    onMoreClick(user, it)
                }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserManagement>() {
        override fun areItemsTheSame(oldItem: UserManagement, newItem: UserManagement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserManagement, newItem: UserManagement): Boolean {
            return oldItem == newItem
        }
    }
}
