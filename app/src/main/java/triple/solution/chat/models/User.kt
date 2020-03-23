package triple.solution.chat.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User(val uid: String, val userName: String, val photoProfileUrl: String) : Parcelable {
    constructor() : this("", "", "")
}