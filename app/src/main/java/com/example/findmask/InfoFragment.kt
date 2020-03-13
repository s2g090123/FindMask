package com.example.findmask


import android.os.Bundle
import android.text.Html
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_info.view.*

/**
 * A simple [Fragment] subclass.
 */
class InfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_info, container, false)
        val item1 = "<font color=#ffffff>●&nbsp;身分證末一碼</font>" +
                "<font color=#dede17>單數星期一、三、五</font>" +
                "<font color=#ffffff>；</font>" +
                "<font color=#dede17><br>&ensp;&nbsp;雙數星期二、四、六</font>" +
                "<font color=#ffffff>可憑健保卡購買</font>"
        val item2 = "<font color=#ffffff>●&nbsp;</font>" +
                "<font color=#dede17>13歲含以下</font>" +
                "<font color=#ffffff>兒童健保卡不受單雙數限<br>&ensp;&nbsp;制</font>"
        val item3 = "<font color=#ffffff>●&nbsp;</font>" +
                "<font color=#dede17>星期日不限身分證字號</font>" +
                "<font color=#ffffff>，皆可至有營<br>&ensp;&nbsp;業健保特約藥局或衛生所購買</font>"
        val item4 = "<font color=#ffffff>●&nbsp;</font>" +
                "<font color=#dede17>每人每7天限購一次</font>"
        val item5 = "<font color=#ffffff>●&nbsp;每日各販賣點</font>" +
                "<font color=#dede17>成人口罩600片</font>" +
                "<font color=#ffffff>，</font>" +
                "<font color=#dede17>兒童<br>&ensp;&nbsp;口罩200片</font>"
        view.item1.text = Html.fromHtml(item1)
        view.item2.text = Html.fromHtml(item2)
        view.item3.text = Html.fromHtml(item3)
        view.item4.text = Html.fromHtml(item4)
        view.item5.text = Html.fromHtml(item5)

        return view
    }


}
