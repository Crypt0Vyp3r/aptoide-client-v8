/*
 * modified at 2017
 */

package cm.aptoide.pt.downloadmanager.test

import cm.aptoide.pt.downloadmanager.Download
import cm.aptoide.pt.downloadmanager.DownloadRequestsCreator


class ExampleDownloadTest {

    private var downloadRepository: cm.aptoide.pt.downloadmanager.DownloadRepository? = null
    private var downloadRequestsCreator: DownloadRequestsCreator? = null

    @org.junit.Before
    fun preparationBeforeEachMethod() {
        downloadRepository = org.mockito.Mockito.mock<cm.aptoide.pt.downloadmanager.DownloadRepository>(cm.aptoide.pt.downloadmanager.DownloadRepository::class.java)
        downloadRequestsCreator = DownloadRequestsCreator()
    }

    @org.junit.Test
    fun sampleDownloadCreation() {
        org.mockito.Mockito.`when`(downloadRepository?.get(0)).thenReturn(downloadRequestsCreator?.createObservableDownload())
        val testSubscriber = rx.observers.TestSubscriber<Download>()
        val observableDownload = downloadRepository?.get(0)
        observableDownload?.subscribe(testSubscriber)

        testSubscriber.assertNoErrors()
        val resultDownload = testSubscriber.onNextEvents
        org.junit.Assert.assertEquals("abcd", resultDownload[0].hashCode)
    }
}
